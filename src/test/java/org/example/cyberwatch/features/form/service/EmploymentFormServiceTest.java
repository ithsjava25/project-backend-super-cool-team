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
    @DisplayName("Should successfully create an employment form")
    void createForm_Success() {
        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                "19900101-1234", "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );
        Staff hrStaff = createHrStaff();
        EmploymentForm entity = new EmploymentForm();
        EmploymentFormDTO expectedDto = new EmploymentFormDTO();
        expectedDto.setSocialSecurityNumber("19900101-1234");

        when(formRepository.existsBySocialSecurityNumber("19900101-1234")).thenReturn(false);
        when(staffRepository.existsBySocialSecurityNumber(anyString())).thenReturn(false);
        when(encryptionService.encrypt("19900101-1234")).thenReturn("krypterat-ssn");
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(formRepository.save(any())).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(expectedDto);

        EmploymentFormDTO result = service.createForm(dto, hrStaff);

        assertNotNull(result);
        assertEquals("19900101-1234", result.getSocialSecurityNumber());
        verify(encryptionService, times(2)).encrypt("19900101-1234");// verifiera att kryptering sker
        verify(formRepository).save(any());
    }

    @Test
    @DisplayName("Should encrypt SSN before saving")
    void createForm_SsnIsEncrypted() {
        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                "19900101-1234", "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );
        Staff hrStaff = createHrStaff();
        EmploymentForm entity = new EmploymentForm();

        when(formRepository.existsBySocialSecurityNumber(anyString())).thenReturn(false);
        when(staffRepository.existsBySocialSecurityNumber(anyString())).thenReturn(false);
        when(encryptionService.encrypt("19900101-1234")).thenReturn("krypterat-ssn");
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(formRepository.save(any())).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(new EmploymentFormDTO());

        service.createForm(dto, hrStaff);

        // Verifiera att SSN sattes som krypterat värde på entiteten
        assertEquals("krypterat-ssn", entity.getSocialSecurityNumber());
    }

    @Test
    @DisplayName("Should throw exception if SSN already exists in form repository")
    void createForm_DuplicateSsnInForms_ThrowsException() {
        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                "19900101-1234", "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );

        when(formRepository.existsBySocialSecurityNumber("19900101-1234")).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                service.createForm(dto, createHrStaff())
        );
        verify(formRepository, never()).save(any());
        verify(encryptionService, never()).encrypt(any()); // ska inte ens kryptera
    }

    @Test
    @DisplayName("Should throw exception if SSN already exists in staff repository")
    void createForm_DuplicateSsnInStaff_ThrowsException() {
        CreateEmploymentDTO dto = new CreateEmploymentDTO(
                "19900101-1234", "Alice", "Andersson",
                "alice@test.com", "070", Role.HR, Department.BACKEND, null, null, null
        );

        when(formRepository.existsBySocialSecurityNumber("19900101-1234")).thenReturn(false);
        when(encryptionService.encrypt("19900101-1234")).thenReturn("krypterat-ssn");
        when(staffRepository.existsBySocialSecurityNumber("krypterat-ssn")).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                service.createForm(dto, createHrStaff())
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if user is not creator or admin")
    void updateForm_UnauthorizedUser_ThrowsException() {
        Long formId = 1L;
        Staff creator = new Staff();
        creator.setEmail("owner@cyberwatch.local");
        creator.setId(1L);

        Staff otherHr = new Staff();
        otherHr.setEmail("other@cyberwatch.local");
        otherHr.setId(2L);
        otherHr.setRole(Role.HR);

        EmploymentForm existingForm = new EmploymentForm();
        existingForm.setCreatedBy(creator);
        existingForm.setStatus(ApprovalStatus.PENDING);

        when(formRepository.findById(formId)).thenReturn(Optional.of(existingForm));

        assertThrows(AccessDeniedException.class, () ->
                service.updateFormBeforeApproval(formId, new UpdateEmploymentDTO(), otherHr)
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should change status to REJECTED and set rejector")
    void rejectForm_Success() {
        Long id = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.PENDING);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(id)).thenReturn(Optional.of(form));
        when(mapper.toDTO(form)).thenReturn(new EmploymentFormDTO());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.rejectForm(id, cto);

        assertEquals(ApprovalStatus.REJECTED, form.getStatus());
        verify(formRepository).save(form);
    }

    @Test
    @DisplayName("Should approve, archive to S3 and create new Staff member")
    void approveAndFinalize_Success() throws Exception {
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

        String result = service.approveAndFinalizeEmployment(id, cto);

        assertTrue(result.contains("Employment has been approved"));
        assertEquals(ApprovalStatus.APPROVED, form.getStatus());
        verify(s3Service).uploadJsonData(anyString(), anyString());
        verify(staffRepository).save(newEmployee);
    }

    @Test
    @DisplayName("Should throw exception if form is not PENDING when rejecting")
    void rejectForm_NotPendingStatus_ThrowsException() {
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.APPROVED);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        assertThrows(IllegalStateException.class, () ->
                service.rejectForm(formId, cto)
        );
        verify(formRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if form not found")
    void rejectForm_FormNotFound_ThrowsException() {
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        assertThrows(EmploymentFormNotFound.class, () ->
                service.rejectForm(99L, cto)
        );
    }

    @Test
    @DisplayName("Should throw exception if form is not PENDING when approving")
    void approveAndFinalize_NotPendingStatus_ThrowsException() {
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.APPROVED);
        Staff cto = new Staff();
        cto.setRole(Role.CTO);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        assertThrows(IllegalStateException.class, () ->
                service.approveAndFinalizeEmployment(formId, cto)
        );
        verify(staffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception if approver is not CEO or CTO")
    void approveAndFinalize_WrongRole_ThrowsException() {
        Long formId = 1L;
        EmploymentForm form = new EmploymentForm();
        form.setStatus(ApprovalStatus.PENDING);
        Staff hr = new Staff();
        hr.setRole(Role.HR);

        when(formRepository.findById(formId)).thenReturn(Optional.of(form));

        assertThrows(IllegalStateException.class, () ->
                service.approveAndFinalizeEmployment(formId, hr)
        );
        verify(staffRepository, never()).save(any());
    }
}