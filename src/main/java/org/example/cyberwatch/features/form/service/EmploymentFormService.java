package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentMapper;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.stereotype.Service;

@Service
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
    public void createEmployee(CreateEmploymentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");

        }
        if (dto.getStatus() == null) {
            dto.setStatus(Status.PENDING); // Set default status to PENDING if not provided
        }

        try {
            //Saved successfully
            employmentFormRepository.save(employmentMapper.toEntity(dto));
        } catch (Exception e) {
            // Handle exceptions that may occur during the save operation
            throw new RuntimeException("Failed to save EmploymentForm: " + e.getMessage(), e);
        }
        //3. If status is approved, create Staff entity and save to database

        if (dto.getStatus() == Status.APPROVED) {
            staffRepository.save(employmentMapper.toEntity(dto));
        }

    }
    //view
    //show all employmentforms with status waiting for approval

    //delete
    //When approved by management delete
}
