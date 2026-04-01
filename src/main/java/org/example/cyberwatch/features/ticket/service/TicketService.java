package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.model.TicketResponseDTO;
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

    public TicketService(TicketRepository ticketRepository, StaffRepository staffRepository) {
        this.ticketRepository = ticketRepository;
        this.staffRepository = staffRepository;
    }

    public TicketResponseDTO createTicket(TicketDTO dto) {
        Staff creator = staffRepository.findById(dto.getCreatedById())
                .orElseThrow(() -> new StaffNotFoundException(dto.getCreatedById()));

        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(dto.getPriority());
        ticket.setIssueType(dto.getIssueType());
        ticket.setCreatedBy(creator);
        ticket.setStatus(Status.SUBMITTED);

        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return TicketResponseDTO.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    public TicketResponseDTO advanceTicketStatus(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.advanceStatus();
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO setTicketStatus(Long id, Status newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        validateStatusTransition(ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO reopenTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.reopen();
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO assignTicket(Long ticketId, Long staffId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        ticket.setAssignedTo(staff);
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    private void validateStatusTransition(Status current, Status next) {
        boolean valid = switch (current) {
            case DRAFT            -> next == Status.SUBMITTED;
            case SUBMITTED        -> next == Status.IN_PROGRESS;
            case IN_PROGRESS      -> next == Status.RESOLVED || next == Status.WAITING_FOR_USER;
            case WAITING_FOR_USER -> next == Status.IN_PROGRESS;
            case RESOLVED         -> next == Status.CLOSED;
            case REOPENED         -> next == Status.IN_PROGRESS;
            case CLOSED           -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Ogiltig statusövergång: " + current + " → " + next);
        }
    }
}