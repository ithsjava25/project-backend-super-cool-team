package org.example.cyberwatch.features.form.model;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmploymentMapper {

    public EmploymentFormDTO toDTO(EmploymentForm entity) {
        if (entity == null) return null;
        return new EmploymentFormDTO(
                entity.getId(),
                entity.getSocialSecurityNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getRole(),
                entity.getDepartment(),
                entity.getStatus(),
                entity.getCreatedDate(),
                entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null,
                entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null
        );
    }

    public EmploymentForm toEntity(CreateEmploymentDTO dto) {
        if (dto == null) return null;
        EmploymentForm entity = new EmploymentForm();
        entity.setSocialSecurityNumber(dto.getSocialSecurityNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setRole(dto.getRole());
        entity.setDepartment(dto.getDepartment());
        entity.setStatus(ApprovalStatus.PENDING); // Default status for new forms
        // NOTE: Service layer is responsible for setting HR and ApproverManagement relationships
        return entity;
    }

    public Staff formToStaff(EmploymentForm form) {
        if (form == null) return null;
        Staff staff = new Staff();
        staff.setSocialSecurityNumber(form.getSocialSecurityNumber());
        staff.setFirstName(form.getFirstName());
        staff.setLastName(form.getLastName());
        staff.setEmail(form.getEmail());
        staff.setPhoneNumber(form.getPhoneNumber());
        staff.setRole(form.getRole());
        staff.setDepartment(form.getDepartment());
        return staff;
    }


    public List<EmploymentFormDTO> toDTOList(List<EmploymentForm> entities) {
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

}
