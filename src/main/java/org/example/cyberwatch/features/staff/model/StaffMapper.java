package org.example.cyberwatch.features.staff.model;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffMapper {

    public StaffDTO toDto(Staff entity) {
        return new StaffDTO(
                entity.getId(),
                entity.getSocialSecurityNumber(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getRole(),
                entity.getDepartment()
        );
    }

    public void updateEntity(StaffDTO dto, Staff entity) {
        if (dto == null || entity == null) return;
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setRole(dto.getRole());
        entity.setDepartment(dto.getDepartment());
    }


    public List<StaffDTO> toDTOList(List<Staff> entities) {
        return entities.stream()
                .map(this::toDto)
                .toList();
    }
}
