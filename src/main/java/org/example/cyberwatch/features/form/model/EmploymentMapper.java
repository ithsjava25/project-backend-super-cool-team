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

    public EmploymentForm toEntity(EmploymentFormDTO dto) {
        EmploymentForm entity = new EmploymentForm();
        return mapDtoToEntity(dto, entity);
    }

    //Update an already created entity, without new
    public EmploymentForm updateEntity(EmploymentFormDTO dto, EmploymentForm entity) {
        return mapDtoToEntity(dto, entity);
    }

    private EmploymentForm mapDtoToEntity(EmploymentFormDTO dto, EmploymentForm entity) {
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

    public List<EmploymentFormDTO> toDTOList(List<EmploymentForm> entities) {
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

}
