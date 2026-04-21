package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.StaffMapper;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StaffServiceTest {

    private StaffService staffService;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private StaffMapper staffMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        staffService = new StaffService(staffRepository, staffMapper);
    }

    @Test
    void updateStatus_ShouldUpdateAndReturnStaffDTO() {
        Long staffId = 1L;
        String status = "BUSY";
        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setStatus("ONLINE");

        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setId(staffId);
        staffDTO.setStatus(status);

        when(staffRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(staffRepository.save(any(Staff.class))).thenReturn(staff);
        when(staffMapper.toDto(any(Staff.class))).thenReturn(staffDTO);

        StaffDTO result = staffService.updateStatus(staffId, status);

        assertEquals(status, result.getStatus());
        assertEquals(status, staff.getStatus());
    }
}
