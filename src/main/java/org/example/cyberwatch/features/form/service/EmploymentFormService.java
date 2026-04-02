package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.model.EmploymentMapper;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Status;
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
    public EmploymentFormDTO createEmployee(CreateEmploymentDTO dto) {
        if (dto == null) throw new IllegalArgumentException("CreateEmploymentDTO cannot be null");

//IMPORTANTE: Implement safetynet for duplicated ssn

        if (dto.getStatus() == null) dto.setStatus(Status.PENDING); // Set default status to PENDING if not provided

//NOTE: setHrId() will be based om the logged in HR-staff
        EmploymentForm formEntity = employmentMapper.toEntity(dto);
        EmploymentForm savedForm = employmentFormRepository.save(formEntity);

        if (dto.getStatus() == Status.APPROVED) {
            Staff newStaff = employmentMapper.formToStaff(savedForm);
            staffRepository.save(newStaff);
        }
        return employmentMapper.toDTO(savedForm);
    }

    public List<EmploymentFormDTO> getPendingForms() {
        return employmentMapper.toDTOList(employmentFormRepository.findByStatus(Status.PENDING));
    }

    //view
    //show all employmentforms with status waiting for approval

    //delete
    //When approved by management delete
}
