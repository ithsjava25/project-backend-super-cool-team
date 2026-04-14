package org.example.cyberwatch.features.comment.service;

import org.example.cyberwatch.features.activitylog.service.ActivityLogService;
import org.example.cyberwatch.features.comment.model.Comment;
import org.example.cyberwatch.features.comment.model.CommentDTO;
import org.example.cyberwatch.features.comment.model.CommentResponseDTO;
import org.example.cyberwatch.features.comment.repository.CommentRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final ActivityLogService activityLogService;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          ActivityLogService activityLogService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.activityLogService = activityLogService;
    }

    /**
     * Lägger till en kommentar på ett ärende.
     * Författaren hämtas från den inloggade användaren (SecurityContext via controllern)
     * istället för från request body — förhindrar att någon kan låtsas vara en annan användare.
     */
    public CommentResponseDTO addComment(Long ticketId, CommentDTO dto, Staff author) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setText(dto.getText());
        commentRepository.save(comment);

        activityLogService.logComment(ticket, author, dto.getText());

        return new CommentResponseDTO(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsForTicket(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(CommentResponseDTO::new)
                .toList();
    }
}