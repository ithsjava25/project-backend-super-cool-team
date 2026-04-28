package org.example.cyberwatch.features.form.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.cyberwatch.config.security.EncryptionService;
import org.example.cyberwatch.features.form.dto.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.dto.EmploymentFormDTO;
import org.example.cyberwatch.features.form.dto.UpdateEmploymentDTO;
import org.example.cyberwatch.features.form.exception.EmploymentFormNotFound;
import org.example.cyberwatch.features.form.mapper.EmploymentMapper;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class EmploymentFormService {

    private static final Logger logger = LoggerFactory.getLogger(EmploymentFormService.class);

    private final EmploymentFormRepository employmentFormRepository;
    private final StaffRepository staffRepository;
    private final EmploymentMapper employmentMapper;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    //Create employment form
    @Transactional
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public EmploymentFormDTO createForm(CreateEmploymentDTO form, Staff hrStaff) {
        if (form == null) {
            throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");
        }
        if (hrStaff == null) {
            throw new IllegalArgumentException("HR staff cannot be null");
        }

        validateSsnNotExists(form.getSocialSecurityNumber());

        EmploymentForm formEntity = employmentMapper.toEntity(form);
        formEntity.setStatus(ApprovalStatus.PENDING);
        formEntity.setCreatedBy(hrStaff);
        String encryptedSsn = encryptionService.encrypt(form.getSocialSecurityNumber());
        formEntity.setSocialSecurityNumber(encryptedSsn);
        formEntity.setSsnHash(encryptionService.hmac(form.getSocialSecurityNumber()));

        EmploymentForm savedForm = employmentFormRepository.save(formEntity);
        logger.info("New employment form created with ID: {} by HR staffId={}", savedForm.getId(), hrStaff.getId());

        return applySsnPolicy(savedForm);
    }

    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO', 'ADMIN')")
    public List<EmploymentFormDTO> getFormsByFilterApproval(ApprovalStatus status) {
        List<EmploymentForm> forms;
        if (status != null) {
            forms = employmentFormRepository.findByStatus(status);
        } else {
            forms = employmentFormRepository.findAll();
        }

        // Använd din nya policy-metod på varje element i listan!
        return forms.stream()
                .map(this::applySsnPolicy)
                .toList();
    }


    // Get a single form by ID
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO', 'ADMIN')")
    public EmploymentFormDTO getFormById(Long formId) {
        return applySsnPolicy(findFormById(formId));
    }

    // Update form before approval (only PENDING forms can be updated)
    @Transactional
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')") // Only HR or Admin can update forms before approval
    public EmploymentFormDTO updateFormBeforeApproval(Long formId, UpdateEmploymentDTO updatedForm, Staff loggedInHr) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        if (updatedForm == null) {
            throw new IllegalArgumentException("Updated form cannot be null");
        }

        EmploymentForm existingForm = findFormById(formId);

        // Only PENDING forms can be updated
        if (existingForm.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be updated. Current status: " + existingForm.getStatus());
        }

        boolean isAdmin = loggedInHr.getRole() == Role.ADMIN;
        boolean isCreator = existingForm.getCreatedBy() != null
                && Objects.equals(existingForm.getCreatedBy().getId(), loggedInHr.getId());

        if (!isAdmin && !isCreator) {
            throw new AccessDeniedException("Only the HR staff who created this form or an admin can update it");
        }
        String existingSsnPlain = encryptionService.decrypt(existingForm.getSocialSecurityNumber());
        String newSsnPlain = updatedForm.getSocialSecurityNumber();
        // Check for duplicate SSN if it's changed
        if (!existingSsnPlain.equals(newSsnPlain)) {
            validateSsnNotExists(newSsnPlain);
            existingForm.setSocialSecurityNumber(encryptionService.encrypt(newSsnPlain));
            existingForm.setSsnHash(encryptionService.hmac(newSsnPlain));
        }

        employmentMapper.updateEntity(updatedForm, existingForm);

        logger.info("Form {} updated by staffId={}", formId, loggedInHr.getId());
        return applySsnPolicy(employmentFormRepository.save(existingForm));
    }

    // Reject a form (only PENDING forms can be rejected, and only by management)
    @Transactional
    @PreAuthorize("hasAnyRole('CEO', 'CTO', 'ADMIN')")
    public String rejectForm(Long formId, Staff loggedInRejecter) {

        EmploymentForm form = findFormById(formId);

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be rejected. Current status: " + form.getStatus());
        }

        if (loggedInRejecter.getRole() != Role.CEO
                && loggedInRejecter.getRole() != Role.CTO
                && loggedInRejecter.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only CEO, CTO or ADMIN can reject employment forms");
        }

        form.setStatus(ApprovalStatus.REJECTED);
        form.setApprovedBy(loggedInRejecter);
        try {
            archiveToS3(form);
        } catch (RuntimeException e) {
            logger.error("Failed to archive form {} to S3", formId, e);
            throw e;
        }
        employmentFormRepository.save(form);

        logger.info("Form {} rejected by staffId={}", formId, loggedInRejecter.getId());
        return "Employment form has been rejected";
    }

    // Delete a form (only PENDING forms can be deleted, and only by the HR who created it or an ADMIN)
    @Transactional
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public void deleteForm(Long formId, Staff loggedInDeleter) {
        EmploymentForm form = findFormById(formId);

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be deleted. Current status: " + form.getStatus());
        }

        boolean isAdmin = loggedInDeleter.getRole() == Role.ADMIN;
        boolean isCreator = form.getCreatedBy() != null
                && Objects.equals(form.getCreatedBy().getId(), loggedInDeleter.getId());
        if (!isAdmin && !isCreator) {
            throw new IllegalStateException("Only the HR staff who created this form or admin can delete it");
        }

        employmentFormRepository.deleteById(formId);
        logger.info("Form {} deleted by staffId={}", formId, loggedInDeleter.getId());
    }

    // When approved by management, archive to S3 and add the employee to staff
    @Transactional
    @PreAuthorize("hasAnyRole('CEO', 'CTO', 'ADMIN')")
    public String approveAndFinalizeEmployment(Long formId, Staff loggedInManagement) {
        EmploymentForm form = findFormById(formId);

        if (loggedInManagement.getRole() != Role.CEO && loggedInManagement.getRole() != Role.CTO && loggedInManagement.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only CEO, CTO or ADMIN can approve employment forms");
        }

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be approved. Current status: " + form.getStatus());
        }

        // Create and save new staff first (all DB operations before S3 write)
        Staff newStaff = employmentMapper.formToStaff(form);
        String rawPassword = generateSecurePassword();
        newStaff.setPassword(passwordEncoder.encode(rawPassword));

        // Update form status and save to DB and archive
        form.setApprovedBy(loggedInManagement);
        form.setStatus(ApprovalStatus.APPROVED);
        try {
            archiveToS3(form);
        } catch (RuntimeException e) {
            logger.error("Failed to archive form {} to S3", formId, e);
            throw e;
        }
        newStaff.setEmployedS3Key(form.getEmployedS3Key());
        staffRepository.save(newStaff);
        employmentFormRepository.save(form);

        logger.info("Form {} approved by staffId={}", formId, loggedInManagement.getId());
        //No need to worry, this will be replaced with an email service
        return "Employment has been approved, generated password for new employee: " + rawPassword;

    }

    private EmploymentForm findFormById(Long formId) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        return employmentFormRepository.findById(formId)
                .orElseThrow(() -> new EmploymentFormNotFound("Form not found with id: " + formId));
    }

    private String generateSecurePassword() {
        return RandomStringUtils.secure().nextAlphanumeric(12);
    }

    private void validateSsnNotExists(String ssn) {
        String ssnHash = encryptionService.hmac(ssn);

        // Kontrollera EmploymentForm
        if (employmentFormRepository.existsBySsnHash(ssnHash)) {
            throw new IllegalStateException("An application with this SSN already exists.");
        }

        // Kontrollera Staff
        if (staffRepository.existsBySsnHash(ssnHash)) {
            throw new IllegalStateException("An employee with this SSN already exists.");
        }
    }

    private EmploymentFormDTO applySsnPolicy(EmploymentForm form) {
        EmploymentFormDTO dto = employmentMapper.toDTO(form);
        String decrypted = encryptionService.decrypt(form.getSocialSecurityNumber());
        dto.setSocialSecurityNumber(decrypted);
        return dto;
    }

    private void archiveToS3(EmploymentForm form) {
        try {
            // Rewrite form-data to json
            EmploymentFormDTO archiveDto = employmentMapper.toDTO(form);
            String jsonContent = objectMapper.writeValueAsString(archiveDto);
            String s3Key = "archive/employments/" + form.getId() + ".json";
            s3Service.uploadJsonData(s3Key, jsonContent);
            form.setEmployedS3Key(s3Key);
            logger.info("Form {} archived to S3", form.getId());
        } catch (Exception e) {
            logger.error("Failed to upload form {} to S3", form.getId(), e);
            throw new RuntimeException("Could not upload file to S3: " + e.getMessage(), e);
        }
    }

}