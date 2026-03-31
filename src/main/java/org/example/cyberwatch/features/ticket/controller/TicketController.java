package org.example.cyberwatch.features.ticket.controller;

import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.service.TicketService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody TicketDTO dto) {
        return ResponseEntity.ok(ticketService.createTicket(dto));
    }

    @PostMapping(value = "/{ticketId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return ResponseEntity.ok(ticketService.uploadFile(ticketId, file));
    }
}