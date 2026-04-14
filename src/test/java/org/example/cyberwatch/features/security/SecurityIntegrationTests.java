package org.example.cyberwatch.features.security;

import org.example.cyberwatch.features.ticket.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-jwt-testing-purposes-only"
})
class SecurityIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    S3Service s3Service; // Mocka S3Service för att undvika beroenden på externa tjänster under testerna

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
    @DisplayName("HR should access staff endpoints")
    void hrCanAccessStaff() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isOk());
    }

    // ── Forms ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CONSULTANT")
    @DisplayName("Consultant should not access form endpoints")
    void consultantCannotAccessForms() throws Exception {
        mockMvc.perform(get("/api/forms/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("HR should access form endpoints")
    void hrCanCreateForm() throws Exception {
        mockMvc.perform(post("/api/forms/employment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // 400 pga ogiltig body, men inte 403
    }

}
