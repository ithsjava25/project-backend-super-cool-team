package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.StaffMapper;
import org.example.cyberwatch.features.staff.model.UpdateStaffDTO;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class StaffService {

    private static final Logger logger = LoggerFactory.getLogger(StaffService.class);

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;


    public StaffService(StaffRepository staffRepository, StaffMapper staffMapper) {
        this.staffRepository = staffRepository;
        this.staffMapper = staffMapper;
    }

    public StaffDTO getStaffById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        return staffMapper.toDto(staffRepository.findById(id).orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + id)));
    }

    private List<StaffDTO> getAllStaff() {
        return staffMapper.toDTOList(staffRepository.findAll());
    }

    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public StaffDTO updateStaff(Long staffId, UpdateStaffDTO dto) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Staff to be updated cannot be null");
        }

        Staff existingStaff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + staffId));

        if (!Objects.equals(existingStaff.getEmail(), dto.getEmail())) {
            staffRepository.findByEmail(dto.getEmail()).ifPresent(other -> {
                throw new IllegalStateException("Email is already in use: " + dto.getEmail());
            });
        }

        staffMapper.updateEntity(dto, existingStaff);

        logger.info("Staff {} updated", staffId);
        return staffMapper.toDto(staffRepository.save(existingStaff));
    }

    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public void deleteStaff(Long staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        Staff existingStaff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + staffId));
        staffRepository.delete(existingStaff);
        logger.info("Staff {} deleted", staffId);

    }

    public List<StaffDTO> getStaffByRoleOrDepartment(Role role, Department department) {
        if (role != null) {
            return staffRepository.findByRole(role)
                    .stream().map(staffMapper::toDto).toList();
        } else if (department != null) {
            return staffRepository.findByDepartment(department)
                    .stream().map(staffMapper::toDto).toList();
        }
        return getAllStaff();
    }
}