package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final StaffRepository staffRepository;

    // Spring injicerar repositories automatiskt via konstruktorn
    public TicketService(TicketRepository ticketRepository, StaffRepository staffRepository) {
        this.ticketRepository = ticketRepository;
        this.staffRepository = staffRepository;
    }

    // Skapa ett nytt ärende — startar alltid som SUBMITTED
    public Ticket createTicket(Ticket ticket) {
        ticket.setStatus(Status.SUBMITTED);
        return ticketRepository.save(ticket);
    }

    // Hämta ett specifikt ärende, kastar undantag om det inte finns
    @Transactional(readOnly = true)
    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    // Hämta alla ärenden
    @Transactional(readOnly = true)
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    // Flytta ärendet ett steg framåt i flödet: SUBMITTED → IN_PROGRESS → RESOLVED → CLOSED
    public Ticket advanceTicketStatus(Long id) {
        Ticket ticket = getTicketById(id);
        ticket.advanceStatus(); // Logiken sitter i Ticket entity
        return ticketRepository.save(ticket);
    }

    // Sätt en specifik status direkt — t.ex. WAITING_FOR_USER
    public Ticket setTicketStatus(Long id, Status newStatus) {
        Ticket ticket = getTicketById(id);
        validateStatusTransition(ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);
        return ticketRepository.save(ticket);
    }

    // Återöppna ett stängt eller löst ärende
    public Ticket reopenTicket(Long id) {
        Ticket ticket = getTicketById(id);
        ticket.reopen(); // Logiken sitter i entiteten
        return ticketRepository.save(ticket);
    }

    // Tilldela en handläggare till ett ärende
    public Ticket assignTicket(Long ticketId, Long staffId) {
        Ticket ticket = getTicketById(ticketId);
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff med id " + staffId + " hittades inte"));
        ticket.setAssignedTo(staff);
        return ticketRepository.save(ticket);
    }

    // Kontrollera att statusövergången är tillåten
    private void validateStatusTransition(Status current, Status next) {
        boolean valid = switch (current) {
            case SUBMITTED        -> next == Status.IN_PROGRESS;
            case IN_PROGRESS      -> next == Status.RESOLVED || next == Status.WAITING_FOR_USER;
            case WAITING_FOR_USER -> next == Status.IN_PROGRESS;
            case RESOLVED         -> next == Status.CLOSED;
            case REOPENED         -> next == Status.IN_PROGRESS;
            case CLOSED           -> false;
            default               -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Ogiltig statusövergång: " + current + " → " + next
            );
        }
    }
}