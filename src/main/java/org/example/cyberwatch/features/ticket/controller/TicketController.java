package org.example.cyberwatch.features.ticket.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.model.TicketResponseDTO;
import org.example.cyberwatch.features.ticket.service.TicketService;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<TicketResponseDTO> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> setStatus(@PathVariable Long id,
                                                       @RequestParam Status status) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketResponseDTO> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reopenTicket(id));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketResponseDTO> assign(@PathVariable Long id,
                                                    @RequestParam Long staffId) {
        return ResponseEntity.ok(ticketService.assignTicket(id, staffId));
    }
}