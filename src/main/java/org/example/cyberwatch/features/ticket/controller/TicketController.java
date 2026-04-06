package org.example.cyberwatch.features.ticket.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.AssignTicketDTO;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.model.TicketResponseDTO;
import org.example.cyberwatch.features.ticket.service.TicketService;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody TicketDTO dto) {
        TicketResponseDTO created = ticketService.createTicket(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PatchMapping("/{id}/advance")
    public ResponseEntity<TicketResponseDTO> advanceStatus(@PathVariable Long id,
                                                           @RequestParam Long performedById) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id, performedById));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> setStatus(@PathVariable Long id,
                                                       @RequestParam Status status,
                                                       @RequestParam Long performedById) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status, performedById));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopen(@PathVariable Long id,
                                                    @RequestParam Long performedById) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, performedById));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{ticketId}/assign")
    public ResponseEntity<TicketResponseDTO> assignTicketToStaff(
            @PathVariable Long ticketId,
            @RequestParam Long assignedById,
            @Valid @RequestBody AssignTicketDTO dto) {
        return ResponseEntity.ok(ticketService.assignTicketToStaff(ticketId, dto.getStaffId(), assignedById));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{ticketId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam Long uploadedById,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ticketService.uploadFile(ticketId, uploadedById, file));
        } catch (TicketNotFoundException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "File upload failed"));
        }
    }
}