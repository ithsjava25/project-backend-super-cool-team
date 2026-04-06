package org.example.cyberwatch.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cyberwatch.features.form.controller.EmploymentFormController;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.service.EmploymentFormService;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmploymentFormController.class)
//Bypass security filters for now
class EmploymentFormControllerTests {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper(); //Debug why @Autowired is not working

    @MockitoBean
    EmploymentFormService employmentFormService;

    @Test
    @DisplayName("POST Test creating a new employment form with valid data")
    @WithMockUser(username = "hr_specialist@cyberwatch.com", roles = {"HR"})
    void createEmploymentForm() throws Exception {

        CreateEmploymentDTO request = new CreateEmploymentDTO();
        request.setFirstName("Erik");
        request.setLastName("Svensson");
        request.setSocialSecurityNumber("19900101-1234");
        request.setEmail("erik@example.com");
        request.setPhoneNumber("0799887766");
        request.setRole(Role.CONSULTANT);
        request.setDepartment(Department.BACKEND);
        request.setStatus(ApprovalStatus.PENDING);

        EmploymentFormDTO mockResponse = new EmploymentFormDTO();
        mockResponse.setId(1L);
        mockResponse.setFirstName("Erik");


        when(employmentFormService.createForm(any(), any())).thenReturn(mockResponse);


        mockMvc.perform(post("/api/forms/employment")
                        .with(csrf()) // Include CSRF token for POST request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("Skapa anställningsformulär - Invalid email")
    @WithMockUser(roles = {"HR"})
    void createEmploymentFormInvalidEmail() throws Exception {
        String jsonRequest = """
                {
                    "firstName": "Erik",
                    "lastName": "Svensson",
                    "socialSecurityNumber": "19900101-1234",
                    "email": "invalid-email",
                    "phoneNumber": "0799887766",
                    "role": "CONSULTANT",
                    "department": "BACKEND",
                    "status": "PENDING"
                }
                """;

        mockMvc.perform(post("/api/forms/employment")
                        .with(csrf()) // Include CSRF token for POST request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }
}
