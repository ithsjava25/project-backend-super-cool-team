package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.config.security.EncryptionService;
import org.example.cyberwatch.features.form.dto.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.dto.EmploymentFormDTO;
import org.example.cyberwatch.features.form.dto.UpdateEmploymentDTO;
import org.example.cyberwatch.features.form.exception.EmploymentFormNotFound;
import org.example.cyberwatch.features.form.mapper.EmploymentMapper;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
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
    @Mock
    private EncryptionService encryptionService;  // ny

    @InjectMocks
    private EmploymentFormService service;

    // Hjälpmetod för att slippa upprepa
    private Staff createHrStaff() {
        Staff staff = new Staff();
        staff.setEmail("hr@cyberwatch.local");
        staff.setRole(Role.HR);
        return staff;
    }

    @Test
    @DisplayName("Should successfully create form and ensure SSN is encrypted before saving")
    void createForm_Success() {
        // Arrange
        String rawSsn = "19900101-1234";
        String encryptedSsn = "krypterat-ssn";

        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                rawSsn, "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );
        Staff hrStaff = createHrStaff();
        EmploymentForm entity = new EmploymentForm();

        when(formRepository.findAll()).thenReturn(List.of());
        when(staffRepository.findAll()).thenReturn(List.of());
        when(encryptionService.encrypt(rawSsn)).thenReturn(encryptedSsn);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(formRepository.save(any(EmploymentForm.class))).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(new EmploymentFormDTO());

        // Act
        EmploymentFormDTO result = service.createForm(dto, hrStaff);

        // Assert
        assertNotNull(result);
        verify(encryptionService).encrypt(rawSsn);
        assertEquals(encryptedSsn, entity.getSocialSecurityNumber(),
                "The entity should hold the encrypted SSN when saved");

        verify(formRepository).save(entity);
    }


    @Test
    @DisplayName("Should throw exception if user is not creator or admin")
    void updateForm_UnauthorizedUser_ThrowsException() {
        // Arrange
        Long formId = 1L;
        Staff creator = new Staff();
        creator.setId(1L);
        creator.setEmail("owner@cyberwatch.local");

        Staff otherHr = new Staff();
        otherHr.setEmail("other@cyberwatch.local");
        otherHr.setId(2L);
        otherHr.setRole(Role.HR);

        EmploymentForm existingForm = new EmploymentForm();
        existingForm.setCreatedBy(creator);
        existingForm.setStatus(ApprovalStatus.PENDING);

        when(formRepository.findById(formId)).thenReturn(Optional.of(existingForm));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                service.updateFormBeforeApproval(formId, new UpdateEmploymentDTO(), otherHr)
        );
        verify(formRepository, never()).save(any());
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
        when(mapper.toDTO(form)).thenReturn(new EmploymentFormDTO());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        service.rejectForm(id, cto);

        // Assert
        assertEquals(ApprovalStatus.REJECTED, form.getStatus());
        verify(formRepository).save(form);
    }

    @Test
    @DisplayName("Should approve, archive to S3 and create new Staff member")
    void approveAndFinalize_Success() throws Exception {
        // Arrange
        Long id = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setSocialSecurityNumber("krypterat-ssn");
        form.setStatus(ApprovalStatus.PENDING);

        Staff cto = new Staff();
        cto.setRole(Role.CTO);
        Staff newEmployee = new Staff();

        when(formRepository.findById(id)).thenReturn(Optional.of(form));
        when(mapper.formToStaff(form)).thenReturn(newEmployee);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(mapper.toDTO(form)).thenReturn(new EmploymentFormDTO());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        String result = service.approveAndFinalizeEmployment(id, cto);

        // Assert
        assertTrue(result.contains("Employment has been approved"));
        assertEquals(ApprovalStatus.APPROVED, form.getStatus());
        verify(s3Service).uploadJsonData(anyString(), anyString());
        verify(staffRepository).save(newEmployee);
    }

    @Test
    @DisplayName("Should throw exception if SSN already exists in form repository")
    void createForm_DuplicateSsnInForms_ThrowsException() {
        // Arrange
        Staff creator = new Staff();
        creator.setEmail("owner@cyberwatch.local");
        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                "19900101-1234", "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );
        EmploymentForm existing = new EmploymentForm();
        existing.setSocialSecurityNumber("krypterat-ssn");

        when(formRepository.findAll()).thenReturn(List.of(existing));
        when(encryptionService.decrypt("krypterat-ssn")).thenReturn("19900101-1234");

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.createForm(dto, creator)
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if SSN already exists in staff repository")
    void createForm_DuplicateSsnInStaff_ThrowsException() {
        // Arrange
        Staff creator = new Staff();
        String rawSsn = "19900101-1234";

        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                rawSsn, "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );

        Staff existingStaff = new Staff();
        existingStaff.setSocialSecurityNumber("krypterat-ssn");

        when(formRepository.findAll()).thenReturn(List.of());//no match
        when(staffRepository.findAll()).thenReturn(List.of(existingStaff));
        when(encryptionService.decrypt("krypterat-ssn")).thenReturn("19900101-1234");

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.createForm(dto, creator)
        );

        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if form is not PENDING when updating")
    void updateForm_NotPendingStatus_ThrowsException() {
        // Arrange
        Staff updater = new Staff();
        updater.setEmail("owner@cyberwatch.local");
        Long formId = 1L;
        UpdateEmploymentDTO updateDto = new UpdateEmploymentDTO();
        EmploymentForm existingForm = new EmploymentForm();
        existingForm.setStatus(ApprovalStatus.APPROVED);

        when(formRepository.findById(formId)).thenReturn(Optional.of(existingForm));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.updateFormBeforeApproval(formId, updateDto, updater)
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if form is not PENDING when rejecting")
    void rejectForm_NotPendingStatus_ThrowsException() {
        // Arrange
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.APPROVED);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.rejectForm(formId, cto)
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if form not found")
    void rejectForm_FormNotFound_ThrowsException() {
        // Arrange
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        // Act & Assert
        assertThrows(EmploymentFormNotFound.class, () ->
                service.rejectForm(99L, cto)
        );
    }

    @Test
    @DisplayName("Should throw exception if form is not PENDING when approving")
    void approveAndFinalize_NotPendingStatus_ThrowsException() {
        // Arrange
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.APPROVED);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.approveAndFinalizeEmployment(formId, cto)
        );
        verify(staffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if approver is not CEO, CTO or ADMIN")
    void approveAndFinalize_WrongRole_ThrowsException() {
        // Arrange
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.PENDING);
        Staff hr = new Staff();
        hr.setRole(Role.HR);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                service.approveAndFinalizeEmployment(formId, hr)
        );
        verify(staffRepository, never()).save(any());
    }
}