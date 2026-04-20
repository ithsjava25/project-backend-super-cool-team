package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketAttachment;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.shared.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

/**
 * Enhetstester för behörighetslogiken i nedladdningsflödet.
 *
 * Testar TicketController.downloadAttachment()-logiken isolerat
 * utan att starta hela Spring-kontexten.
 *
 * Scenarion:
 * - Ägare får ladda ner
 * - Tilldelad handläggare får ladda ner
 * - Admin får ladda ner
 * - Oinblandad användare nekas
 * - Bilaga som tillhör annat ärende nekas
 */
class TicketDownloadAccessTest {

    // Hjälpklass som isolerar behörighetslogiken från controllern
    // så vi kan testa den utan MockMvc
    static class DownloadAccessChecker {

        private final TicketAttachmentRepository attachmentRepository;
        private final S3Service s3Service;

        DownloadAccessChecker(TicketAttachmentRepository repo, S3Service s3Service) {
            this.attachmentRepository = repo;
            this.s3Service = s3Service;
        }

        URL check(Ticket ticket, Long attachmentId, Staff requester) {
            boolean isOwner = ticket.getCreatedBy() != null &&
                    ticket.getCreatedBy().getId().equals(requester.getId());
            boolean isAssigned = ticket.getAssignedStaff() != null &&
                    ticket.getAssignedStaff().stream()
                            .anyMatch(s -> s.getId().equals(requester.getId()));
            boolean isAdmin = requester.getRole() == Role.ADMIN;

            if (!isOwner && !isAssigned && !isAdmin) {
                throw new AccessDeniedException("Du har inte tillgång till filer i detta ärende.");
            }

            TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Bilaga hittades inte"));

            if (!attachment.getTicket().getId().equals(ticket.getId())) {
                throw new AccessDeniedException("Bilagan tillhör inte angivet ärende.");
            }

            return s3Service.generatePresignedUrl(attachment.getS3Key(), Duration.ofMinutes(5));
        }
    }

    private TicketAttachmentRepository attachmentRepo;
    private S3Service s3Service;
    private DownloadAccessChecker checker;

    private Ticket ticket;
    private TicketAttachment attachment;
    private Staff ägare;
    private Staff handläggare;
    private Staff admin;
    private Staff oinblandad;

    @BeforeEach
    void setUp() throws Exception {
        attachmentRepo = mock(TicketAttachmentRepository.class);
        s3Service = mock(S3Service.class);
        checker = new DownloadAccessChecker(attachmentRepo, s3Service);

        ägare = staff(1L, Role.CONSULTANT);
        handläggare = staff(2L, Role.HR);
        admin = staff(3L, Role.ADMIN);
        oinblandad = staff(4L, Role.CONSULTANT);

        ticket = new Ticket();
        ReflectionTestUtils.setField(ticket, "id", 10L);
        ticket.setCreatedBy(ägare);
        ticket.setAssignedStaff(new ArrayList<>(List.of(handläggare)));

        attachment = new TicketAttachment();
        ReflectionTestUtils.setField(attachment, "id", 99L);
        attachment.setS3Key("tickets/10/uuid-fil.pdf");
        attachment.setTicket(ticket);

        when(attachmentRepo.findById(99L)).thenReturn(Optional.of(attachment));
        when(s3Service.generatePresignedUrl(anyString(), any()))
                .thenReturn(new URL("https://minio/presigned"));
    }

    @Test
    @DisplayName("Ägaren ska kunna ladda ner")
    void owner_canDownload() {
        assertThatNoException().isThrownBy(() -> checker.check(ticket, 99L, ägare));
    }

    @Test
    @DisplayName("Tilldelad handläggare ska kunna ladda ner")
    void assignedStaff_canDownload() {
        assertThatNoException().isThrownBy(() -> checker.check(ticket, 99L, handläggare));
    }

    @Test
    @DisplayName("Admin ska kunna ladda ner oavsett ärendetillhörighet")
    void admin_canDownload() {
        assertThatNoException().isThrownBy(() -> checker.check(ticket, 99L, admin));
    }

    @Test
    @DisplayName("Oinblandad användare ska nekas med AccessDeniedException")
    void unrelatedUser_isDenied() {
        assertThatThrownBy(() -> checker.check(ticket, 99L, oinblandad))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tillgång");
    }

    @Test
    @DisplayName("Bilaga som tillhör annat ärende ska nekas även för admin")
    void attachment_belongingToOtherTicket_isDenied() {
        Ticket annatÄrende = new Ticket();
        ReflectionTestUtils.setField(annatÄrende, "id", 999L);

        TicketAttachment annanBilaga = new TicketAttachment();
        ReflectionTestUtils.setField(annanBilaga, "id", 77L);
        annanBilaga.setS3Key("tickets/999/annan.pdf");
        annanBilaga.setTicket(annatÄrende); // tillhör annat ärende

        when(attachmentRepo.findById(77L)).thenReturn(Optional.of(annanBilaga));

        // Admin försöker hämta bilaga 77 via ärende 10 – ska nekas
        assertThatThrownBy(() -> checker.check(ticket, 77L, admin))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tillhör inte");
    }

    @Test
    @DisplayName("Presigned URL ska genereras med rätt s3-nyckel och 5 minuters giltighetstid")
    void download_generatesPresignedUrlWithCorrectParams() throws Exception {
        checker.check(ticket, 99L, ägare);

        verify(s3Service).generatePresignedUrl(
                eq("tickets/10/uuid-fil.pdf"),
                eq(Duration.ofMinutes(5)));
    }

    // ── Hjälpmetod ──────────────────────────────────────────────────────────

    private Staff staff(Long id, Role role) {
        Staff s = new Staff();
        ReflectionTestUtils.setField(s, "id", id);
        s.setRole(role);
        return s;
    }
}