package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.activitylog.service.ActivityLogService;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketAttachment;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceUploadTest {

    @Mock TicketRepository ticketRepository;
    @Mock StaffRepository staffRepository;
    @Mock TicketAttachmentRepository ticketAttachmentRepository;
    @Mock S3Client s3Client;
    @Mock ActivityLogService activityLogService;

    @InjectMocks
    TicketService ticketService;

    private Ticket ticket;
    private Staff uploader;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketService, "bucket", "test-bucket");

        ticket = new Ticket();
        ReflectionTestUtils.setField(ticket, "id", 1L);

        uploader = new Staff();
        ReflectionTestUtils.setField(uploader, "id", 10L);
        uploader.setFirstName("Test");
        uploader.setLastName("Användare");
        uploader.setRole(Role.CONSULTANT);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(staffRepository.findById(10L)).thenReturn(Optional.of(uploader));
    }

    // Stubbar S3 – anropas bara i tester som faktiskt når uppladdningssteget
    private void stubS3() {
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenReturn(null);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
    }

    @Test
    @DisplayName("Uppladdning sparar s3Key och returnerar downloadUrl – aldrig fileUrl")
    void uploadFile_savesS3KeyAndReturnsDownloadUrl() throws Exception {
        stubS3();
        MockMultipartFile file = new MockMultipartFile(
                "file", "rapport.pdf", "application/pdf", "pdfinnehåll".getBytes());

        ArgumentCaptor<TicketAttachment> attachmentCaptor =
                ArgumentCaptor.forClass(TicketAttachment.class);
        when(ticketAttachmentRepository.save(attachmentCaptor.capture()))
                .thenAnswer(inv -> {
                    TicketAttachment a = inv.getArgument(0);
                    ReflectionTestUtils.setField(a, "id", 99L);
                    return a;
                });

        Map<String, Object> response = ticketService.uploadFile(1L, 10L, file);

        assertThat(response).containsKey("downloadUrl");
        assertThat(response).doesNotContainKey("fileUrl");
        assertThat(response.get("downloadUrl").toString())
                .isEqualTo("/api/tickets/1/attachments/99/download");

        TicketAttachment sparad = attachmentCaptor.getValue();
        assertThat(sparad.getS3Key()).isNotBlank();
        assertThat(sparad.getS3Key()).startsWith("attachments/1/");
        assertThat(sparad.getFileName()).isEqualTo("rapport.pdf");

        verify(activityLogService).logFileUpload(eq(ticket), eq(uploader), eq("rapport.pdf"));
    }

    @Test
    @DisplayName("S3-nyckeln innehåller aldrig path-traversal-tecken")
    void uploadFile_sanitizesFileName() throws Exception {
        stubS3();
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd", "text/plain", "data".getBytes());

        ArgumentCaptor<TicketAttachment> captor =
                ArgumentCaptor.forClass(TicketAttachment.class);
        when(ticketAttachmentRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        ticketService.uploadFile(1L, 10L, file);

        String sparatFilnamn = captor.getValue().getFileName();
        assertThat(sparatFilnamn).doesNotContain("..");
        assertThat(sparatFilnamn).doesNotContain("/");
        assertThat(sparatFilnamn).doesNotContain("\\");
    }

    @Test
    @DisplayName("Tom fil ska kasta RuntimeException")
    void uploadFile_withEmptyFile_throwsException() {
        // S3 stubbar behövs inte – undantag kastas innan S3 anropas
        MockMultipartFile tomFil = new MockMultipartFile(
                "file", "tom.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> ticketService.uploadFile(1L, 10L, tomFil))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tom");
    }

    @Test
    @DisplayName("Filen ska faktiskt laddas upp till S3")
    void uploadFile_callsS3PutObject() throws Exception {
        stubS3();
        MockMultipartFile file = new MockMultipartFile(
                "file", "dokument.txt", "text/plain", "innehåll".getBytes());

        when(ticketAttachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.uploadFile(1L, 10L, file);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
    @Test
    @DisplayName("Otillåten filtyp ska kasta RuntimeException")
    void uploadFile_withInvalidMimeType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "data".getBytes());

        assertThatThrownBy(() -> ticketService.uploadFile(1L, 10L, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Otillåten filtyp");
    }

    @Test
    @DisplayName("Null Content-Type ska kasta RuntimeException")
    void uploadFile_withNullContentType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fil.pdf", null, "data".getBytes());

        assertThatThrownBy(() -> ticketService.uploadFile(1L, 10L, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Otillåten filtyp");
    }

    @Test
    @DisplayName("För stor fil ska kasta RuntimeException")
    void uploadFile_withOversizedFile_throwsException() {
        // Skapar en fil på 10 MB + 1 byte
        byte[] tooLarge = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "stor.pdf", "application/pdf", tooLarge);

        assertThatThrownBy(() -> ticketService.uploadFile(1L, 10L, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("för stor");
    }
}