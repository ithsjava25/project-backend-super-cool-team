package org.example.cyberwatch.features.form.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmploymentMapper {

    public EmploymentFormDTO toDTO(EmploymentForm entity) {
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
        entity.setStatus(dto.getStatus());
        // NOTE: Service layer is responsible for setting HR and ApproverManagement relationships
        return entity;
    }
    //Update an already created entity, without new
    public void updateEntity(UpdateEmploymentDTO dto, EmploymentForm entity) {
        if (dto == null || entity == null) return;

        if (dto.getStatus() != null)
            entity.setStatus(dto.getStatus());
    }


    public List<EmploymentFormDTO> toDTOList(List<EmploymentForm> entities) {
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

}
