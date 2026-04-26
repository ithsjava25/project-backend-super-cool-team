package org.example.cyberwatch;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    // ── Staff ──────────────────────────────────────────

    @Test
    @DisplayName("HR should be able to access staff endpoints")
    void hrCanAccessStaff() throws Exception {
        Staff hrUser = new Staff();
        hrUser.setFirstName("Test");
        hrUser.setLastName("HR");
        hrUser.setEmail("hr@cyberwatch.com");
        hrUser.setRole(Role.HR);

        // Skapa en Authentication-token.
        // hrUser som "principal" (identitet).
        var auth = new UsernamePasswordAuthenticationToken(
                hrUser,                   // Principal (det som hamnar i @AuthenticationPrincipal)
                null,                     // Credentials (behövs inte här)
                List.of(new SimpleGrantedAuthority("ROLE_HR")) // Rollen för filter-säkerheten
        );

        mockMvc.perform(get("/api/staff")
                        .with(authentication(auth)))
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
                        .content("{}").with(csrf()))
                .andExpect(status().isBadRequest()); // 400 pga ogiltig body, men inte 403
    }

    @Test
    @DisplayName("HR should not be able to approve forms")
    void hrCannotApproveForm() throws Exception {
        Staff hrUser = new Staff();
        hrUser.setId(10L);
        hrUser.setEmail("hr@cyberwatch.local");
        hrUser.setRole(Role.HR);

        //Skapa en Authentication-token där din Staff är "Principal"
        var auth = new UsernamePasswordAuthenticationToken(
                hrUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_HR"))
        );

        // 3. Skicka med denna auth i perform-anropet
        mockMvc.perform(post("/api/forms/1/approve")
                        .with(csrf())
                        .with(authentication(auth))) //Staff som principal
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
