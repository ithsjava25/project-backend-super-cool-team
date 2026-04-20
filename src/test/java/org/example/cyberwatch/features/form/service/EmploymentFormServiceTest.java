package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.features.form.dto.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.dto.EmploymentFormDTO;
import org.example.cyberwatch.features.form.dto.UpdateEmploymentDTO;
import org.example.cyberwatch.features.form.mapper.EmploymentMapper;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmploymentFormServiceTest {
    @Mock
    private EmploymentFormRepository formRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private EmploymentMapper mapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmploymentFormService service;

    @Test
    @DisplayName("Should successfully create an employment form")
    void createForm_Success() {
        // Arrange
        CreateEmploymentDTO dto = new CreateEmploymentDTO("19900101-1234", "Alice", "Andersson", "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null);
        Staff hrStaff = new Staff();
        hrStaff.setEmail("hr@cyberwatch.local");

        EmploymentForm entity = new EmploymentForm();
        EmploymentFormDTO expectedDto = new EmploymentFormDTO();
        expectedDto.setSocialSecurityNumber("19900101-1234");

        when(formRepository.existsBySocialSecurityNumber(anyString())).thenReturn(false);
        when(staffRepository.existsBySocialSecurityNumber(anyString())).thenReturn(false);
        when(staffRepository.findByEmail("hr@cyberwatch.local")).thenReturn(Optional.of(hrStaff));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(formRepository.save(any(EmploymentForm.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(expectedDto);

        // Act
        EmploymentFormDTO result = service.createForm(dto, "hr@cyberwatch.local");

        // Assert
        assertNotNull(result);
        assertEquals("19900101-1234", result.getSocialSecurityNumber());
        verify(formRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw exception if another HR tries to update the form")
    void updateForm_UnauthorizedUser_ThrowsException() {
        // Arrange
        Long formId = 1L;
        UpdateEmploymentDTO updateDto = new UpdateEmploymentDTO();

        Staff creator = new Staff();
        creator.setEmail("owner@cyberwatch.local");

        EmploymentForm existingForm = new EmploymentForm();
        existingForm.setCreatedBy(creator);
        existingForm.setStatus(ApprovalStatus.PENDING);

        when(formRepository.findById(formId)).thenReturn(Optional.of(existingForm));

        // Act & Assert
        assertThrows(StaffNotFoundException.class, () -> {
            service.updateFormBeforeApproval(formId, updateDto, "hacker@cyberwatch.local");
        });
    }

    @Test
    @DisplayName("Should change status to REJECTED and set rejector")
    void rejectForm_Success() {
        // Arrange
        Long id = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.PENDING);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(id)).thenReturn(Optional.of(form));
        when(staffRepository.findByEmail("cto@cyberwatch.local")).thenReturn(Optional.of(cto));

        // Act
        service.rejectForm(id, "cto@cyberwatch.local");

        // Assert
        assertEquals(ApprovalStatus.REJECTED, form.getStatus());
        verify(staffRepository).findByEmail("cto@cyberwatch.local");
        verify(formRepository).save(form);
    }

    @Test
    @DisplayName("Should approve, archive to S3 and create new Staff member")
    void approveAndFinalize_Success() throws Exception {
        // Arrange
        Long id = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setSocialSecurityNumber("1990-1234");
        form.setStatus(ApprovalStatus.PENDING);

        Staff cto = new Staff();
        cto.setRole(Role.CTO);
        Staff newEmployee = new Staff();

        when(formRepository.findById(id)).thenReturn(Optional.of(form));
        when(staffRepository.findByEmail(anyString())).thenReturn(Optional.of(cto));
        when(mapper.formToStaff(form)).thenReturn(newEmployee);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");

        when(mapper.toDTO(form)).thenReturn(new EmploymentFormDTO());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        String result = service.approveAndFinalizeEmployment(id, "manager@cyberwatch.local");

        // Assert
        assertTrue(result.contains("Employment has been approved"));
        assertEquals(ApprovalStatus.APPROVED, form.getStatus());
        verify(s3Service).uploadJsonData(anyString(), anyString());
        verify(staffRepository).save(newEmployee);
    }
}