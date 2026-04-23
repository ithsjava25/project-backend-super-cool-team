package org.example.cyberwatch.features.staff.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.StaffMapper;
import org.example.cyberwatch.features.staff.model.UpdateStaffDTO;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {


    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffMapper staffMapper;

    @InjectMocks
    private StaffService staffService;

    private Staff newStaff;
    private StaffDTO newStaffDTO;


    @BeforeEach
    void setUp() {
        newStaff = new Staff();
        newStaff.setId(1L);
        newStaff.setFirstName("Anna");
        newStaff.setLastName("Svensson");
        newStaff.setEmail("anna@cyberwatch.se");
        newStaff.setRole(Role.HR);
        newStaff.setDepartment(Department.BACKEND);

        newStaffDTO = new StaffDTO(1L, null, "Anna", "Svensson",
                "anna@cyberwatch.se", null, Role.HR, Department.BACKEND, "profil.png", "ONLINE");
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

    @Test
    @DisplayName("Should return DTO when staff exists")
    void getStaffByIdStaffExists() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(newStaff));
        when(staffMapper.toDto(newStaff)).thenReturn(newStaffDTO);

        StaffDTO result = staffService.getStaffById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("anna@cyberwatch.se");
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when staff not found")
    void getStaffByIdNotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.getStaffById(99L))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when id is null")
    void getStaffByIdIsNull() {
        assertThatThrownBy(() -> staffService.getStaffById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should update and return DTO when valid input")
    void updateStaffWhenValid() {
        UpdateStaffDTO dto = new UpdateStaffDTO("Anna", "Nilsson",
                "anna@cyberwatch.se", "0701234567", Role.HR, Department.BACKEND);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(newStaff));
        when(staffRepository.save(newStaff)).thenReturn(newStaff);
        when(staffMapper.toDto(newStaff)).thenReturn(newStaffDTO);

        StaffDTO result = staffService.updateStaff(1L, dto);

        verify(staffMapper).updateEntity(dto, newStaff);
        verify(staffRepository).save(newStaff);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when updating non-existent staff")
    void updateStaffNotFound() {
        UpdateStaffDTO dto = new UpdateStaffDTO();
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.updateStaff(99L, dto))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete staff when exists")
    void deleteStaffExists() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(newStaff));

        staffService.deleteStaff(1L);

        verify(staffRepository).delete(newStaff);
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when deleting non-existent staff")
    void deleteStaffNotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.deleteStaff(99L))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should filter by role when role is provided")
    void getStaffByRoleOrDepartmentFilteredByRole() {
        when(staffRepository.findByRole(Role.HR)).thenReturn(List.of(newStaff));
        when(staffMapper.toDto(newStaff)).thenReturn(newStaffDTO);

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(Role.HR, null);

        assertThat(result).hasSize(1);
        verify(staffRepository, times(1)).findByRole(Role.HR);
        verify(staffRepository, never()).findByDepartment(any());
    }

    @Test
    @DisplayName("Should filter by department when department is provided")
    void getStaffByRoleOrDepartmentFilteredByDepartment() {
        when(staffRepository.findByDepartment(Department.BACKEND)).thenReturn(List.of(newStaff));
        when(staffMapper.toDto(newStaff)).thenReturn(newStaffDTO);

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(null, Department.BACKEND);

        assertThat(result).hasSize(1);
        verify(staffRepository).findByDepartment(Department.BACKEND);
    }

    @Test
    @DisplayName("Should have OFFLINE as default status when creating new Staff entity")
    void shouldHaveDefaultOfflineStatus() {
        Staff staff = new Staff();
        assertThat(staff.getStatus()).isEqualTo("OFFLINE");
    }

    @Test
    @DisplayName("Should return all staff when no filter is provided")
    void getStaffByRoleOrDepartment() {
        when(staffRepository.findAll()).thenReturn(List.of(newStaff));
        when(staffMapper.toDTOList(any())).thenReturn(List.of(newStaffDTO));

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(null, null);

        assertThat(result).hasSize(1);
        verify(staffRepository).findAll();
    }
}
