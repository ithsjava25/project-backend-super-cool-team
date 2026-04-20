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
 *
 * Behörighetsmodell (se TicketSecurityService för detaljer):
 *
 * Alla inloggade          → skapa ärende, se ärendelista (filtrerad per roll)
 * Förhöjda roller         → se och ändra alla ärenden (ADMIN, CEO, CTO)
 * Ägare / tilldelad       → se och ändra sina egna ärenden (HR, PM, CONSULTANT)
 * Endast ADMIN            → tilldela och radera ärenden
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

    // Alla inloggade kan skapa ärenden
    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody TicketDTO dto) {
        String email = getAuthenticatedStaff().getEmail();
        TicketResponseDTO created = ticketService.createTicket(dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Hämtar tickets med valfri filtrering.
     * Alla inloggade kan nå endpointen – service-lagret filtrerar resultatet per roll.
     * Förhöjda roller (ADMIN, CEO, CTO) ser alla ärenden.
     * Standardroller ser bara ärenden de skapat eller är tilldelade till.
     * Bilagor exkluderas för att undvika N+1-queries i listvyn.
     */
    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets(@ModelAttribute TicketFilterParams filters) {
        return ResponseEntity.ok(ticketService.getFilteredTickets(filters, getAuthenticatedStaff()));
    }

    // Förhöjda roller + ägare/tilldelad
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // Förhöjda roller + ägare/tilldelad
    @PreAuthorize("@ticketSecurity.canAccessByCode(authentication, #ticketCode)")
    @GetMapping("/code/{ticketCode}")
    public ResponseEntity<TicketResponseDTO> getTicketByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.getTicketByCode(ticketCode));
    }

    // Förhöjda roller + ägare/tilldelad – flytta ärendet till nästa status i livscykeln
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/advance")
    public ResponseEntity<TicketResponseDTO> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id, getAuthenticatedStaffId()));
    }

    // Förhöjda roller + ägare/tilldelad – sätt en specifik status
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> setStatus(@PathVariable Long id,
                                                       @RequestParam Status status) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status, getAuthenticatedStaffId()));
    }

    // Förhöjda roller + ägare/tilldelad – återöppna stängt ärende
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #id)")
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, getAuthenticatedStaffId()));
    }

    // Endast ADMIN – tilldela handläggare till ett ärende
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{ticketId}/assign")
    public ResponseEntity<TicketResponseDTO> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketDTO dto) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, dto.getStaffIds(), getAuthenticatedStaffId()));
    }

    // Endast ADMIN – permanent borttagning av ärende
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    // Förhöjda roller + ägare/tilldelad – ladda upp bilaga till ärende
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @PostMapping(value = "/{ticketId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ticketService.uploadFile(ticketId, getAuthenticatedStaffId(), file));
        } catch (TicketNotFoundException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uppladdning misslyckades"));
        }
    }

    /**
     * Genererar en tidsbegränsad, signerad nedladdningslänk för en bilaga.
     *
     * @PreAuthorize kontrollerar behörighet mot ärendet innan vi ens hämtar bilagan.
     * Flöde: autentisering → @PreAuthorize (403 om nekad) → hämta bilaga → presigned URL → 302
     */
    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @GetMapping("/{ticketId}/attachments/{attachmentId}/download")
    public ResponseEntity<Void> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {

        // Behörighet är redan kontrollerad av @PreAuthorize ovan
        TicketAttachment attachment = ticketAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        // Extra kontroll: bilagan måste tillhöra rätt ärende för att förhindra IDOR
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