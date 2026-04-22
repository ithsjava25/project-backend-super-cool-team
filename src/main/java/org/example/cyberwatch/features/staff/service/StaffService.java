package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.config.security.EncryptionService;
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
import java.util.Set;

@Service
public class StaffService {

    private static final Logger logger = LoggerFactory.getLogger(StaffService.class);

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("ONLINE", "OFFLINE", "AWAY", "BUSY");

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;
    private final EncryptionService encryptionService;

    public StaffService(StaffRepository staffRepository, StaffMapper staffMapper, EncryptionService encryptionService) {
        this.staffRepository = staffRepository;
        this.staffMapper = staffMapper;
        this.encryptionService = encryptionService;
    }

    public StaffDTO getStaffById(Long id, Staff requester) {
        if (id == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + id));

        StaffDTO dto = staffMapper.toDto(staff);

        //Avgör vad av personnumret som får visas för användaren
        if (requester.getRole() == Role.ADMIN || requester.getRole() == Role.HR) {
            dto.setSocialSecurityNumber(encryptionService.decrypt(staff.getSocialSecurityNumber()));
        } else {
            dto.setSocialSecurityNumber(encryptionService.maskLastFour(staff.getSocialSecurityNumber()));
        }

        return dto;
    }

    public StaffDTO getUserStaff(Staff user) {
        if (user == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }
        StaffDTO dto = staffMapper.toDto(user);
        dto.setSocialSecurityNumber(encryptionService.maskLastFour(user.getSocialSecurityNumber()));
        return dto;
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

    public List<StaffDTO> getStaffByRoleOrDepartment(Role role, Department department, Staff requester) {

        List<Staff> staffList;
        if (role != null) {
            staffList = staffRepository.findByRole(role);
        } else if (department != null) {
            staffList = staffRepository.findByDepartment(department);
        } else {
            staffList = staffRepository.findAll(); // ersätter getAllStaff()
        }

        return staffList.stream()
                .map(s -> {
                    StaffDTO dto = staffMapper.toDto(s);
                    if (requester.getRole() == Role.ADMIN || requester.getRole() == Role.HR) {
                        dto.setSocialSecurityNumber(encryptionService.decrypt(s.getSocialSecurityNumber()));
                    } else {
                        dto.setSocialSecurityNumber(encryptionService.maskLastFour(s.getSocialSecurityNumber()));
                    }
                    return dto;
                })
                .toList();
    }

    public StaffDTO updateStatus(Long staffId, String status) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }

        if (status == null || status.isBlank() || !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + staffId));

        staff.setStatus(status);
        logger.info("Staff {} status updated to {}", staffId, status);

        return staffMapper.toDto(staffRepository.save(staff));
    }
}