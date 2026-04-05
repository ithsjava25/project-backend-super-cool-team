package org.example.cyberwatch.features.form.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.model.EmploymentMapper;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmploymentFormService {

    private final EmploymentFormRepository employmentFormRepository;
    private final StaffRepository staffRepository;
    private final EmploymentMapper employmentMapper;

    public EmploymentFormService(EmploymentFormRepository employmentFormRepository, StaffRepository staffRepository, EmploymentMapper employmentMapper) {
        this.employmentFormRepository = employmentFormRepository;
        this.staffRepository = staffRepository;
        this.employmentMapper = employmentMapper;
    }

    //Create employment & insert in staff.java
    @Transactional
    public EmploymentFormDTO createForm(CreateEmploymentDTO form) {
        if (form == null) throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");

        //Implement safetynet for duplicated ssn
        if (employmentFormRepository.existsBySocialSecurityNumber(form.getSocialSecurityNumber())) {
            throw new IllegalStateException("An application with this SSN already exists.");
        }

        //NOTE: setHrId() will be based om the logged in HR-staff
        EmploymentForm formEntity = employmentMapper.toEntity(form);
        // Set default status to PENDING if not provided
        if (form.getStatus() == null)
            form.setStatus(ApprovalStatus.PENDING);

        return employmentMapper.toDTO(employmentFormRepository.save(formEntity));
    }


    //view: show all employmentforms with status waiting for approval
    public List<EmploymentFormDTO> getPendingForms() {
        //if list is empty show empty list in UI
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(ApprovalStatus.PENDING));
    }


    //When approved by management delete the form from the database, and add the employee to staff
    public void approveAndFinalizeEmployment(Long formId) {
        if (formId == null) throw new IllegalArgumentException("EmploymentFormDTO cannot be null");

        EmploymentForm form = employmentFormRepository.findById(formId)
                .orElseThrow(() -> new EntityNotFoundException("Form not found with id: " + formId));

        if (form.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only PENDING forms can be approved.");
        }

        Staff newStaff = employmentMapper.formToStaff(form);
        staffRepository.save(newStaff);

        employmentFormRepository.delete(form);

    }
}
