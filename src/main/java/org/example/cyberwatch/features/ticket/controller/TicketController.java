package org.example.cyberwatch.features.ticket.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.exception.AttachmentNotFoundException;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.*;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.service.S3Service;
import org.example.cyberwatch.features.ticket.service.TicketService;
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

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST-controller för ärendehantering.
 */
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

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(@ModelAttribute TicketFilterParams filters) {
        return ResponseEntity.ok(ticketService.getFilteredTickets(filters, getAuthenticatedStaff()));
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PreAuthorize("@ticketSecurity.canAccessByCode(authentication, #ticketCode)")
    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketResponseDTO> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode));
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/advance")
    public ResponseEntity<TicketResponseDTO> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id, getAuthenticatedStaffId()));
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> setStatus(@PathVariable Long id,
                                                       @RequestParam Status status) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status, getAuthenticatedStaffId()));
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, getAuthenticatedStaffId()));
    }

    // Ersatt hasRole('ADMIN') med isAdmin() så att nekade försök loggas med staffId
    @PreAuthorize("@ticketSecurity.isAdmin(authentication)")
    @PutMapping("/{ticketId}/assign")
    public ResponseEntity<TicketResponseDTO> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketDTO dto) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, dto.getStaffIds(), getAuthenticatedStaffId()));
    }

    // Ersatt hasRole('ADMIN') med isAdmin() så att nekade försök loggas med staffId
    @PreAuthorize("@ticketSecurity.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @PostMapping(value = "/{ticketId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ticketService.uploadFile(ticketId, getAuthenticatedStaffId(), file));
        } catch (TicketNotFoundException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Uppladdning misslyckades på grund av ett tekniskt fel."));
        }
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<Void> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {

        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new AccessDeniedException("Bilagan tillhör inte angivet ärende.");
        }

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