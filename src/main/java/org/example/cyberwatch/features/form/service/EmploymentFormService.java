package org.example.cyberwatch.features.form.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.cyberwatch.features.form.model.*;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    //Create employment form
    @Transactional
    public EmploymentFormDTO createForm(CreateEmploymentDTO form, String loggedInHr) {
        if (form == null) {
            throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");
        }

        validateSsnNotExists(form.getSocialSecurityNumber());

        //NOTE: Set HR based on logged in HR-staff
        Staff hrStaff = staffRepository.findByEmail(loggedInHr)
                .orElseThrow(() -> {
                    logger.error("HR staff not found with email: {}", loggedInHr);
                    return new EntityNotFoundException("HR staff not found with username: " + loggedInHr);
                });

        EmploymentForm formEntity = employmentMapper.toEntity(form);
        // Set default status to PENDING if not provided
        if (formEntity.getStatus() == null) {
            formEntity.setStatus(ApprovalStatus.PENDING);
        }
        formEntity.setCreatedBy(hrStaff);

        EmploymentFormDTO savedForm = employmentMapper.toDTO(employmentFormRepository.save(formEntity));
        logger.info("New employment form created with ID: {} by HR: {}", savedForm.getId(), loggedInHr);

        return savedForm;
    }

    //view: show all employmentforms with status
    public List<EmploymentFormDTO> getFormsByStatus(ApprovalStatus status) {
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(status));
    }

    public List<EmploymentFormDTO> getPendingForms() {
        return getFormsByStatus(ApprovalStatus.PENDING);
    }

    public List<EmploymentFormDTO> getApprovedForms() {
        return getFormsByStatus(ApprovalStatus.APPROVED);
    }

    // Get a single form by ID
    public EmploymentFormDTO getFormById(Long formId) {
        return employmentMapper.toDTO(findFormById(formId));
    }

    // Search and filter forms by SSN, department, and status
    public List<EmploymentFormDTO> searchAndFilterForms(String socialSecurityNumber, Department department, ApprovalStatus status) {
        List<EmploymentForm> forms = employmentFormRepository.findAll();

        return forms.stream()
                .filter(form -> socialSecurityNumber == null || form.getSocialSecurityNumber().contains(socialSecurityNumber))
                .filter(form -> department == null || form.getDepartment().equals(department))
                .filter(form -> status == null || form.getStatus().equals(status))
                .map(employmentMapper::toDTO)
                .toList();
    }

    // Update form before approval (only PENDING forms can be updated)
    @Transactional
    public EmploymentFormDTO updateFormBeforeApproval(Long formId, UpdateEmploymentDTO updatedForm, String loggedInHrEmail) {
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

        // Only the HR who created the form can update it
        if (existingForm.getCreatedBy() == null || !existingForm.getCreatedBy().getEmail().equals(loggedInHrEmail)) {
            throw new IllegalStateException("Only the HR staff who created this form can update it");
        }

        // Check for duplicate SSN if it's changed
        if (!existingForm.getSocialSecurityNumber().equals(updatedForm.getSocialSecurityNumber())) {
            validateSsnNotExists(updatedForm.getSocialSecurityNumber());
        }

        employmentMapper.updateEntity(updatedForm, existingForm);

        logger.info("Form {} updated by HR {}", formId, loggedInHrEmail);
        return employmentMapper.toDTO(employmentFormRepository.save(existingForm));
    }

    // Reject a form with a reason
    @Transactional
    public String rejectForm(Long formId, String rejectionReason, String loggedInManagementEmail) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank");
        }

        EmploymentForm form = findFormById(formId);

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be rejected. Current status: " + form.getStatus());
        }

        Staff rejector = staffRepository.findByEmail(loggedInManagementEmail)
                .orElseThrow(() -> new EntityNotFoundException("Rejector not found"));

        form.setStatus(ApprovalStatus.REJECTED);
        form.setApprovedBy(rejector);
        employmentFormRepository.save(form);
        logger.info("Form {} rejected by {}", formId, loggedInManagementEmail);
        return "Employment form has been rejected. Reason: " + rejectionReason;
    }

    // Delete a form (only PENDING forms can be deleted, and only by HR who created it or management)
    @Transactional
    public void deleteForm(Long formId, String loggedInEmail) {
        EmploymentForm form = findFormById(formId);

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be deleted. Current status: " + form.getStatus());
        }

        Staff requester = staffRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!form.getCreatedBy().getEmail().equals(loggedInEmail)
                && requester.getRole() != Role.CEO
                && requester.getRole() != Role.CTO) {
            throw new IllegalStateException("Only the HR staff who created this form or management can delete it");
        }

        employmentFormRepository.deleteById(formId);
        logger.info("Form {} deleted by {}", formId, loggedInEmail);
    }

    // When approved by management, archive to S3 and add the employee to staff
    @Transactional
    public String approveAndFinalizeEmployment(Long formId, String loggedInManagement) {
        EmploymentForm form = findFormById(formId);

        Staff approver = staffRepository.findByEmail(loggedInManagement)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be approved. Current status: " + form.getStatus());
        }

        // Create and save new staff first (all DB operations before S3 write)
        Staff newStaff = employmentMapper.formToStaff(form);
        newStaff.setEmployedS3Key(form.getEmployedS3Key());
        String rawPassword = generateSecurePassword();
        newStaff.setPassword(passwordEncoder.encode(rawPassword));
        staffRepository.save(newStaff);

        // Update form status and save to DB
        form.setApprovedBy(approver);
        form.setStatus(ApprovalStatus.APPROVED);
        employmentFormRepository.save(form);

        // Archive to S3 LAST, after all DB operations are committed
        // This ensures S3 only gets written if the transaction succeeds
        try {
            archiveToS3(form);
        } catch (RuntimeException e) {
            logger.error("Failed to archive form {} to S3", formId, e);
            throw e;
        }

        logger.info("Form {} approved by {}", formId, loggedInManagement);
        //No need to worry, this will be replaced with an email service
        return "Employment has been approved, generated password for new employee: " + rawPassword;

    }

    private String generateSecurePassword() {
        return RandomStringUtils.secure().nextAlphanumeric(12);
    }

    private EmploymentForm findFormById(Long formId) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        return employmentFormRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException("Form not found with id: " + formId));
    }

    private void validateSsnNotExists(String ssn) {
        if (employmentFormRepository.existsBySocialSecurityNumber(ssn)) {
            throw new IllegalStateException("An application with this SSN already exists.");
        }
        if (staffRepository.existsBySocialSecurityNumber(ssn)) {
            throw new IllegalStateException("An employee with this SSN already exists.");
        }
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
