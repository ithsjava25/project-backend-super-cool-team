package org.example.cyberwatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Säkerhetstester för fil-endpoints.
 *
 * Testar att:
 * - Ej inloggad användare får 401 på upload och download
 * - Inloggad användare utan Staff-principal får 403
 *   (eftersom getAuthenticatedStaff() kräver att principal är en Staff-instans)
 *
 * S3Client och S3Presigner mockas så att testerna inte behöver
 * en körande MinIO-instans.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TicketAttachmentSecurityTest {

    @Autowired
    MockMvc mockMvc;

    // Mockas så att Spring inte försöker ansluta till S3 vid uppstart
    @MockitoBean
    S3Client s3Client;

    @MockitoBean
    S3Presigner s3Presigner;

    // ── Upload ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ej inloggad ska inte kunna ladda upp filer")
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "innehåll".getBytes());

        mockMvc.perform(multipart("/api/tickets/1/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("Inloggad användare utan Staff-principal ska få 403 vid upload")
    void upload_withMockUserNotStaff_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "innehåll".getBytes());

        // @WithMockUser sätter en String som principal, inte en Staff-instans.
        // getAuthenticatedStaff() kastar AccessDeniedException → 403.
        mockMvc.perform(multipart("/api/tickets/1/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── Download ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ej inloggad ska inte kunna ladda ner filer")
    void download_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets/1/attachments/1/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CONSULTANT")
    @DisplayName("Inloggad användare utan Staff-principal ska få 403 vid download")
    void download_withMockUserNotStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/tickets/1/attachments/1/download"))
                .andExpect(status().isForbidden());
    }
}