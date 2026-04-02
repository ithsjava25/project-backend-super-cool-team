package org.example.cyberwatch.features.staff.model;

import org.springframework.stereotype.Component;

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

    public Staff toEntity(StaffDTO dto) {
        Staff staff = new Staff();
        dto.setFirstName(dto.getFirstName());
        dto.setLastName(dto.getLastName());
        dto.setEmail(dto.getEmail());
        dto.setPhoneNumber(dto.getPhoneNumber());
        dto.setRole(dto.getRole());
        dto.setDepartment(dto.getDepartment());
        return staff;
    }
}
