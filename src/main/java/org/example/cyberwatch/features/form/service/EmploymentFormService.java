package org.example.cyberwatch.features.form.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.model.EmploymentMapper;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmploymentFormService {

    private static final Logger logger = LoggerFactory.getLogger(EmploymentFormService.class);

    private final EmploymentFormRepository employmentFormRepository;
    private final StaffRepository staffRepository;
    private final EmploymentMapper employmentMapper;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;


    public EmploymentFormService(EmploymentFormRepository employmentFormRepository,
                                 StaffRepository staffRepository,
                                 EmploymentMapper employmentMapper,
                                 S3Service s3Service,
                                 ObjectMapper objectMapper,
                                 PasswordEncoder passwordEncoder) {
        this.employmentFormRepository = employmentFormRepository;
        this.staffRepository = staffRepository;
        this.employmentMapper = employmentMapper;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    //Create employment form
    @Transactional
    public EmploymentFormDTO createForm(CreateEmploymentDTO form, String loggedInHr) {
        if (form == null) {
            throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");
        }

        //Implement safetynet for duplicated ssn
        if (employmentFormRepository.existsBySocialSecurityNumber(form.getSocialSecurityNumber())) {
            logger.warn("Attempt to create form with duplicate SSN: {}", form.getSocialSecurityNumber());
            throw new IllegalStateException("An application with this SSN already exists.");
        }

        if (staffRepository.existsBySocialSecurityNumber(form.getSocialSecurityNumber())) {
            logger.warn("Attempt to create form with SSN already employed: {}", form.getSocialSecurityNumber());
            throw new IllegalStateException("An employee with this SSN already exists.");
        }

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

    //view: show all employmentforms with status waiting for approval
    public List<EmploymentFormDTO> getPendingForms() {
        //if list is empty show empty list in UI
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(ApprovalStatus.PENDING));
    }

    public List<EmploymentFormDTO> getApprovedForms() {
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(ApprovalStatus.APPROVED));
    }

    // Get a single form by ID
    public EmploymentFormDTO getFormById(Long formId) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> {
                    logger.warn("Form not found with id: {}", formId);
                    return new EntityNotFoundException("Form not found with id: " + formId);
                });
        return employmentMapper.toDTO(form);
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
    public EmploymentFormDTO updateFormBeforeApproval(Long formId, CreateEmploymentDTO updatedForm, String loggedInHrEmail) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        if (updatedForm == null) {
            throw new IllegalArgumentException("Updated form cannot be null");
        }

        EmploymentForm existingForm = employmentFormRepository.findById(formId)
                .orElseThrow(() -> {
                    logger.warn("Form not found with id: {}", formId);
                    return new EntityNotFoundException("Form not found with id: " + formId);
                });

        // Only PENDING forms can be updated
        if (existingForm.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be updated. Current status: " + existingForm.getStatus());
        }

        // Only the HR who created the form can update it
        if (!existingForm.getCreatedBy().getEmail().equals(loggedInHrEmail)) {
            logger.warn("Unauthorized update attempt on form {} by {}", formId, loggedInHrEmail);
            throw new IllegalStateException("Only the HR staff who created this form can update it");
        }

        // Check for duplicate SSN if it's changed
        if (!existingForm.getSocialSecurityNumber().equals(updatedForm.getSocialSecurityNumber())) {
            if (employmentFormRepository.existsBySocialSecurityNumber(updatedForm.getSocialSecurityNumber())) {
                throw new IllegalStateException("An application with this SSN already exists.");
            }
            if (staffRepository.existsBySocialSecurityNumber(updatedForm.getSocialSecurityNumber())) {
                throw new IllegalStateException("An employee with this SSN already exists.");
            }
        }

        // Update fields - flytta till mapper
        existingForm.setFirstName(updatedForm.getFirstName());
        existingForm.setLastName(updatedForm.getLastName());
        existingForm.setEmail(updatedForm.getEmail());
        existingForm.setPhoneNumber(updatedForm.getPhoneNumber());
        existingForm.setRole(updatedForm.getRole());
        existingForm.setDepartment(updatedForm.getDepartment());
        existingForm.setSocialSecurityNumber(updatedForm.getSocialSecurityNumber());

        logger.info("Form {} updated by HR {}", formId, loggedInHrEmail);
        return employmentMapper.toDTO(employmentFormRepository.save(existingForm));
    }

    // Reject a form with a reason
    @Transactional
    public String rejectForm(Long formId, String rejectionReason, String loggedInManagementEmail) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank");
        }

        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> {
                    logger.warn("Form not found with id: {}", formId);
                    return new EntityNotFoundException("Form not found with id: " + formId);
                });

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be rejected. Current status: " + form.getStatus());
        }

        Staff rejector = staffRepository.findByEmail(loggedInManagementEmail)
                .orElseThrow(() -> new EntityNotFoundException("Rejector not found"));

        form.setStatus(ApprovalStatus.REJECTED);
        form.setApprovedBy(rejector); // Store who rejected it
        employmentFormRepository.save(form);

        logger.info("Form {} rejected by {} with reason: {}", formId, loggedInManagementEmail, rejectionReason);
        return "Employment form has been rejected. Reason: " + rejectionReason;
    }

    // Delete a form (only PENDING forms can be deleted, and only by HR who created it or management)
    @Transactional
    public void deleteForm(Long formId, String loggedInEmail) {
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }

        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> {
                    logger.warn("Form not found with id: {}", formId);
                    return new EntityNotFoundException("Form not found with id: " + formId);
                });

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be deleted. Current status: " + form.getStatus());
        }

        Staff requester = staffRepository.findByEmail(loggedInEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Only the HR who created it or management can delete
        if (!form.getCreatedBy().getEmail().equals(loggedInEmail) && !requester.getRole().name().equals("MANAGEMENT")) {
            logger.warn("Unauthorized deletion attempt on form {} by {}", formId, loggedInEmail);
            throw new IllegalStateException("Only the HR staff who created this form or management can delete it");
        }

        employmentFormRepository.deleteById(formId);
        logger.info("Form {} deleted by {}", formId, loggedInEmail);
    }

    // When approved by management, archive to S3 and add the employee to staff
    @Transactional
    public String approveAndFinalizeEmployment(Long formId, String loggedInManagement) {
        if (formId == null) throw new IllegalArgumentException("Form ID cannot be null");

        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> {
                    logger.warn("Form not found with id: {}", formId);
                    return new EntityNotFoundException("Form not found with id: " + formId);
                });

        Staff approver = staffRepository.findByEmail(loggedInManagement)
                .orElseThrow(() -> {
                    logger.error("Approver not found with email: {}", loggedInManagement);
                    return new EntityNotFoundException("Approver not found");
                });

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be approved. Current status: " + form.getStatus());
        }

        form.setApprovedBy(approver);
        form.setStatus(ApprovalStatus.APPROVED);

        try {
            archiveToS3(form);
        } catch (RuntimeException e) {
            logger.error("Failed to archive form {} to S3", formId, e);
            throw new RuntimeException("Failed to archive form to S3: " + e.getMessage(), e);
        }

        employmentFormRepository.save(form);

        Staff newStaff = employmentMapper.formToStaff(form);
        String rawPassword = generateSecurePassword();
        String hashedPassword = passwordEncoder.encode(rawPassword);
        newStaff.setPassword(hashedPassword);

        staffRepository.save(newStaff);

        logger.info("Employment form {} approved and finalized by {}. New employee: {}", formId, loggedInManagement, newStaff.getEmail());

        //Will be replaced by sending an email to the newly employed
        return "Employment has been approved, generated password for new employee: " + rawPassword;

    }

    private String generateSecurePassword() {
        return RandomStringUtils.random(12,
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*");
    }

    private void archiveToS3(EmploymentForm form) {
        try {
            // Rewrite form-data to json
            EmploymentFormDTO archiveDto = employmentMapper.toDTO(form);
            String jsonContent = objectMapper.writeValueAsString(archiveDto);

            // Name the file
            String s3Key = "archive/employments/" + form.getSocialSecurityNumber() + ".json";

            // Send json-data to S3
            s3Service.uploadJsonData(s3Key, jsonContent);

            // save key to the json-data
            form.setEmployedS3Key(s3Key);
            logger.info("Form {} successfully archived to S3 at {}", form.getId(), s3Key);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize form {} to JSON", form.getId(), e);
            throw new RuntimeException("Could not create JSON-file for S3: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to upload form {} to S3", form.getId(), e);
            throw new RuntimeException("Could not upload file to S3: " + e.getMessage(), e);
        }
    }

}
