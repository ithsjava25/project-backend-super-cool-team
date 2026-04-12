package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.StaffMapper;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.springframework.stereotype.Service;

@Service
public class StaffService {

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
}
