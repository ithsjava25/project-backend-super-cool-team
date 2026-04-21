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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffMapper staffMapper;
    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private StaffService staffService;

    private Staff staff;
    private StaffDTO staffDTO;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        staff.setId(1L);
        staff.setFirstName("Anna");
        staff.setLastName("Svensson");
        staff.setEmail("anna@cyberwatch.se");
        staff.setRole(Role.HR);
        staff.setDepartment(Department.BACKEND);
        staff.setSocialSecurityNumber("krypterat-ssn");

        staffDTO = new StaffDTO(1L, null, "Anna", "Svensson",
                "anna@cyberwatch.se", null, Role.HR, Department.BACKEND);
    }

    @Test
    @DisplayName("Should return masked SSN for non-admin")
    void getStaffById_NonAdmin_ReturnsMaskedSsn() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.maskLastFour("krypterat-ssn")).thenReturn("19900101-****");

        StaffDTO result = staffService.getStaffById(1L, Role.HR);

        assertThat(result.getSocialSecurityNumber()).isEqualTo("19900101-****");
        verify(encryptionService).maskLastFour("krypterat-ssn");
        verify(encryptionService, never()).decrypt(any());
    }

    @Test
    @DisplayName("Should return decrypted SSN for admin")
    void getStaffById_Admin_ReturnsDecryptedSsn() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.decrypt("krypterat-ssn")).thenReturn("19900101-1234");

        StaffDTO result = staffService.getStaffById(1L, Role.ADMIN);

        assertThat(result.getSocialSecurityNumber()).isEqualTo("19900101-1234");
        verify(encryptionService).decrypt("krypterat-ssn");
        verify(encryptionService, never()).maskLastFour(any());
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when staff not found")
    void getStaffById_NotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.getStaffById(99L, Role.HR))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when id is null")
    void getStaffById_NullId() {
        assertThatThrownBy(() -> staffService.getStaffById(null, Role.HR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should update and return DTO when valid input")
    void updateStaff_Valid() {
        UpdateStaffDTO dto = new UpdateStaffDTO("Anna", "Nilsson",
                "anna@cyberwatch.se", "0701234567", Role.HR, Department.BACKEND);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(staffRepository.save(staff)).thenReturn(staff);
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);

        StaffDTO result = staffService.updateStaff(1L, dto);

        verify(staffMapper).updateEntity(dto, staff);
        verify(staffRepository).save(staff);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when updating non-existent staff")
    void updateStaff_NotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.updateStaff(99L, new UpdateStaffDTO()))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete staff when exists")
    void deleteStaff_Exists() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));

        staffService.deleteStaff(1L);

        verify(staffRepository).delete(staff);
    }

    @Test
    @DisplayName("Should throw StaffNotFoundException when deleting non-existent staff")
    void deleteStaff_NotFound() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.deleteStaff(99L))
                .isInstanceOf(StaffNotFoundException.class);
    }

    @Test
    @DisplayName("Should filter by role and mask SSN for non-admin")
    void getStaffByRoleOrDepartment_FilterByRole_MaskedSsn() {
        when(staffRepository.findByRole(Role.HR)).thenReturn(List.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.maskLastFour("krypterat-ssn")).thenReturn("19900101-****");

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(Role.HR, null, Role.HR);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSocialSecurityNumber()).isEqualTo("19900101-****");
        verify(staffRepository).findByRole(Role.HR);
    }

    @Test
    @DisplayName("Should filter by role and decrypt SSN for admin")
    void getStaffByRoleOrDepartment_FilterByRole_DecryptedSsn() {
        when(staffRepository.findByRole(Role.HR)).thenReturn(List.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.decrypt("krypterat-ssn")).thenReturn("19900101-1234");

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(Role.HR, null, Role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSocialSecurityNumber()).isEqualTo("19900101-1234");
    }

    @Test
    @DisplayName("Should filter by department")
    void getStaffByRoleOrDepartment_FilterByDepartment() {
        when(staffRepository.findByDepartment(Department.BACKEND)).thenReturn(List.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.maskLastFour(any())).thenReturn("19900101-****");

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(null, Department.BACKEND, Role.HR);

        assertThat(result).hasSize(1);
        verify(staffRepository).findByDepartment(Department.BACKEND);
    }

    @Test
    @DisplayName("Should return all staff when no filter")
    void getStaffByRoleOrDepartment_NoFilter() {
        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(staffMapper.toDto(staff)).thenReturn(staffDTO);
        when(encryptionService.maskLastFour(any())).thenReturn("19900101-****");

        List<StaffDTO> result = staffService.getStaffByRoleOrDepartment(null, null, Role.HR);

        assertThat(result).hasSize(1);
        verify(staffRepository).findAll();
    }
}