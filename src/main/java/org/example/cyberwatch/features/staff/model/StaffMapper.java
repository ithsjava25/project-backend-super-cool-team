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

    //Not usable now but could be used in future if staff wants to update their info
    public Staff toEntity(StaffDTO dto) {
        Staff staff = new Staff();
        staff.setId(dto.getId());
        staff.setSocialSecurityNumber(dto.getSocialSecurityNumber());
        staff.setFirstName(dto.getFirstName());
        staff.setLastName(dto.getLastName());
        staff.setEmail(dto.getEmail());
        staff.setPhoneNumber(dto.getPhoneNumber());
        staff.setRole(dto.getRole());
        staff.setDepartment(dto.getDepartment());
        return staff;
    public void updateEntity(UpdateStaffDTO dto, Staff entity) {
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
