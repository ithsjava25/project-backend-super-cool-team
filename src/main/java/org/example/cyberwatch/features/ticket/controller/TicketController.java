package org.example.cyberwatch.features.ticket.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.service.TicketService;
import org.example.cyberwatch.shared.model.enums.Status;
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

    // POST /api/tickets — Skapa nytt ärende
    @PostMapping
    public ResponseEntity<Ticket> createTicket(@Valid @RequestBody TicketDTO dto) {
        return ResponseEntity.ok(ticketService.createTicket(dto));
    }

    // GET /api/tickets — Hämta alla ärenden
    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // GET /api/tickets/5 — Hämta ett specifikt ärende
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // PATCH /api/tickets/5/advance — Flytta ärendet ett steg framåt
    @PatchMapping("/{id}/advance")
    public ResponseEntity<Ticket> advanceStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.advanceTicketStatus(id));
    }

    // PATCH /api/tickets/5/status — Sätt en specifik status, t.ex. WAITING_FOR_USER
    @PatchMapping("/{id}/status")
    public ResponseEntity<Ticket> setStatus(@PathVariable Long id, @RequestParam Status status) {
        return ResponseEntity.ok(ticketService.setTicketStatus(id, status));
    }

    // PATCH /api/tickets/5/reopen — Återöppna ett stängt ärende
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<Ticket> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.reopenTicket(id));
    }

    // PATCH /api/tickets/5/assign?staffId=2 — Tilldela handläggare
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Ticket> assign(@PathVariable Long id, @RequestParam Long staffId) {
        return ResponseEntity.ok(ticketService.assignTicket(id, staffId));
    }
}