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

    //Not usable now but could be used in future if staff wants to update their info
    public Staff toEntity(StaffDTO dto) {
        Staff staff = new Staff();
        staff.setFirstName(dto.getFirstName());
        staff.setLastName(dto.getLastName());
        staff.setEmail(dto.getEmail());
        staff.setPhoneNumber(dto.getPhoneNumber());
        staff.setRole(dto.getRole());
        staff.setDepartment(dto.getDepartment());
        return staff;
    }
}
