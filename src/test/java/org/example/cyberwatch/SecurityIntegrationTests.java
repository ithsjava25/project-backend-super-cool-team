package org.example.cyberwatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    // ── Staff ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CONSULTANT")
    @DisplayName("Consultant should not access staff endpoints")
    void consultantCannotAccessStaff() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("HR should be able to access staff endpoints")
    void hrCanAccessStaff() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CTO")
    @DisplayName("CTO should not be able to delete a staff")
    void ctoCannotDeleteStaff() throws Exception {
        mockMvc.perform(delete("/api/staff/1").with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ── Forms ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CONSULTANT")
    @DisplayName("Consultant should not be able to access form endpoints")
    void consultantCannotAccessForms() throws Exception {
        mockMvc.perform(get("/api/forms").param("status", "PENDING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("HR should access form endpoints and get 400 for bad request in body")
    void hrCanCreateForm() throws Exception {
        mockMvc.perform(post("/api/forms/employment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // 400 pga ogiltig body, men inte 403
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("HR should not be able to approve forms")
    void hrCannotApproveForm() throws Exception {
        mockMvc.perform(post("/api/forms/1/approve")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
