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

        return toDtoWithSsnPolicy(staff, requester);
    }

    public StaffDTO getUserStaff(Staff user) {
        if (user == null) {
            throw new IllegalArgumentException("Staff cannot be null");
        }
        return toMaskedDto(user);
    }

    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public StaffDTO updateStaff(Long staffId, UpdateStaffDTO dto, Staff requester) {
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
        Staff savedStaff = staffRepository.save(existingStaff);

        logger.info("Staff {} updated", staffId);
        return toDtoWithSsnPolicy(savedStaff, requester); // Returnerar maskat SSN efter uppdatering
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
            staffList = staffRepository.findAll();
        }

        return staffList.stream()
                .map(s -> toDtoWithSsnPolicy(s, requester))
                .toList();
    }

    public StaffDTO updateStatus(Long staffId, String status, Staff requester) {
        if (staffId == null) {
            throw new IllegalArgumentException("Staff ID cannot be null");
        }

        if (status == null || status.isBlank() || !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + staffId));

        staff.setStatus(status);
        Staff savedStaff = staffRepository.save(staff);
        logger.info("Staff {} status updated to {}", staffId, status);

        return toDtoWithSsnPolicy(savedStaff, requester); // Returnerar maskat SSN efter statusuppdatering
    }


    // ADMIN/HR får se hela, andra får se maskat.
    private StaffDTO toDtoWithSsnPolicy(Staff staff, Staff requester) {
        StaffDTO dto = staffMapper.toDto(staff);
        String rawEncryptedSsn = staff.getSocialSecurityNumber();

        if (requester != null && (requester.getRole() == Role.ADMIN || requester.getRole() == Role.HR)) {
            dto.setSocialSecurityNumber(encryptionService.decrypt(rawEncryptedSsn));
        } else {
            dto.setSocialSecurityNumber(encryptionService.maskLastFour(rawEncryptedSsn));
        }
        return dto;
    }

    private StaffDTO toMaskedDto(Staff staff) {
        StaffDTO dto = staffMapper.toDto(staff);
        dto.setSocialSecurityNumber(encryptionService.maskLastFour(staff.getSocialSecurityNumber()));
        return dto;
    }
}