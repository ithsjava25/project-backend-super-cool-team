package org.example.cyberwatch.features.ticket.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.*;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.features.ticket.service.TicketService;
import org.example.cyberwatch.shared.model.enums.Role;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final S3Service s3Service;
    private final TicketAttachmentRepository ticketAttachmentRepository;

    public TicketController(TicketService ticketService,
                            S3Service s3Service,
                            TicketAttachmentRepository ticketAttachmentRepository) {
        this.ticketService = ticketService;
        this.s3Service = s3Service;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody TicketDTO dto) {
        String email = getAuthenticatedStaff().getEmail();
        TicketResponseDTO created = ticketService.createTicket(dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Hämtar tickets med valfri filtrering via query params.
     * Exempel:
     * GET /api/tickets              → alla tickets
     * GET /api/tickets?status=IN_PROGRESS
     * GET /api/tickets?assignedToId=3
     */
    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(@ModelAttribute TicketFilterParams filters) {
        return ResponseEntity.ok(ticketService.getFilteredTickets(filters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketResponseDTO> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode));
    }

    @PatchMapping("/{id}/advance")
    public ResponseEntity<TicketResponseDTO> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id, getAuthenticatedStaffId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> setStatus(@PathVariable Long id,
                                                       @RequestParam Status status) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status, getAuthenticatedStaffId()));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, getAuthenticatedStaffId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{ticketId}/assign")
    public ResponseEntity<TicketResponseDTO> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketDTO dto) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, dto.getStaffIds(), getAuthenticatedStaffId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{ticketId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ticketService.uploadFile(ticketId, getAuthenticatedStaffId(), file));
        } catch (TicketNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uppladdning misslyckades"));
        }
    }

    /**
     * Genererar en tidsbegränsad, signerad nedladdningslänk för en bilaga.
     *
     * Flöde:
     * 1. Kontrollera att användaren är inloggad (via Spring Security)
     * 2. Kontrollera att användaren har tillgång till ärendet (ägare, handläggare eller admin)
     * 3. Hämta bilagan och generera en presigned URL giltig i 5 minuter
     * 4. Returnera HTTP 302 redirect till den signerade URL:en
     *
     * Direkta S3-URL:er exponeras aldrig mot klienten. Efter 5 minuter
     * upphör länken att fungera och en ny måste genereras via detta endpoint.
     */
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<Void> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {

        Staff requester = getAuthenticatedStaff();

        // Hämta ärendet och kontrollera att användaren har tillgång
        Ticket ticket = ticketService.getTicketEntityById(ticketId);

        boolean isOwner = ticket.getCreatedBy() != null &&
                ticket.getCreatedBy().getId().equals(requester.getId());
        boolean isAssigned = ticket.getAssignedStaff() != null &&
                ticket.getAssignedStaff().stream()
                        .anyMatch(s -> s.getId().equals(requester.getId()));
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAssigned && !isAdmin) {
            throw new AccessDeniedException("Du har inte tillgång till filer i detta ärende.");
        }

        // Hämta bilagan och verifiera att den tillhör rätt ärende
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Bilaga hittades inte: " + attachmentId));

        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new AccessDeniedException("Bilagan tillhör inte angivet ärende.");
        }

        // Generera en presigned URL giltig i 5 minuter och redirecta dit
        URL presignedUrl = s3Service.generatePresignedUrl(
                attachment.getS3Key(),
                Duration.ofMinutes(5));

        try {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(presignedUrl.toURI())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Kunde inte generera nedladdningslänk.", e);
        }
    }

    private Long getAuthenticatedStaffId() {
        return getAuthenticatedStaff().getId();
    }

    private Staff getAuthenticatedStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Ingen autentiserad användare.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Staff staff) {
            return staff;
        }
        throw new AccessDeniedException("Unauthorized");
    }
}