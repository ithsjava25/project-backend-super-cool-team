package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.StaffMapper;
import org.example.cyberwatch.features.staff.model.UpdateStaffDTO;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<StaffDTO> getAllStaff() {
        return staffMapper.toDTOList(staffRepository.findAll());
    }

    public StaffDTO updateStaff(Long staffId, UpdateStaffDTO dto) {
        if (staffId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Updated form cannot be null");
        }

        Staff existingForm = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException("Staff not found with id: " + staffId));
        staffMapper.updateEntity(dto, existingForm);

        logger.info("Form {} updated", staffId);
        return staffMapper.toDto(staffRepository.save(existingForm));
    }
}
