package org.example.cyberwatch.features.form.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.model.EmploymentMapper;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmploymentFormService {

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

    //Create employment & insert in staff.java
    @Transactional
    public EmploymentFormDTO createForm(CreateEmploymentDTO form, String loggedInHr) {
        if (form == null) throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");

        //Implement safetynet for duplicated ssn
        if (employmentFormRepository.existsBySocialSecurityNumber(form.getSocialSecurityNumber())) {
            throw new IllegalStateException("An application with this SSN already exists.");
        }

        if (staffRepository.existsBySocialSecurityNumber(form.getSocialSecurityNumber())) {
            throw new IllegalStateException("An employee with this SSN already exists.");
        }

        //NOTE: setHrId() will be based om the logged in HR-staff
        Staff hrStaff = staffRepository.findByEmail(loggedInHr)
                .orElseThrow(() -> new EntityNotFoundException("HR staff not found with username: " + loggedInHr));
        EmploymentForm formEntity = employmentMapper.toEntity(form);
        // Set default status to PENDING if not provided
        if (formEntity.getStatus() == null)
            formEntity.setStatus(ApprovalStatus.PENDING);
        formEntity.setCreatedBy(hrStaff);

        return employmentMapper.toDTO(employmentFormRepository.save(formEntity));
    }

    //visa en enstaka form för att kunna uppdatera den?


    //view: show all employmentforms with status waiting for approval
    public List<EmploymentFormDTO> getPendingForms() {
        //if list is empty show empty list in UI
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(ApprovalStatus.PENDING));
    }

    public List<EmploymentFormDTO> getApprovedForms() {
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(ApprovalStatus.APPROVED));
    }

    //When approved by management delete the form from the database, and add the employee to staff
    @Transactional
    public void approveAndFinalizeEmployment(Long formId, String loggedInManagement) {
        if (formId == null) throw new IllegalArgumentException("EmploymentFormDTO cannot be null");

        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException("Form not found with id: " + formId));

        Staff approver = staffRepository.findByEmail(loggedInManagement)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found"));

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be approved.");
        }

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
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kunde inte skapa JSON-fil för molnarkivering", e);
        }

        form.setApprovedBy(approver);
        form.setStatus(ApprovalStatus.APPROVED);
        employmentFormRepository.save(form);

        Staff newStaff = employmentMapper.formToStaff(form);
        String rawPassword = "DefaultPassword123!"; // TODO: Generate a secure random password and communicate it to the new employee
        newStaff.setPassword(rawPassword);
        staffRepository.save(newStaff);

//Lösenord för den nya staffen?
    }

}
