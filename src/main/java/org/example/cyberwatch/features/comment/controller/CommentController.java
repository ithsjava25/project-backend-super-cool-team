package org.example.cyberwatch.features.comment.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.comment.model.CommentDTO;
import org.example.cyberwatch.features.comment.model.CommentResponseDTO;
import org.example.cyberwatch.features.comment.service.CommentService;
import org.example.cyberwatch.features.staff.model.Staff;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CommentDTO dto,
            // Hämtar den inloggade Staff-instansen direkt från SecurityContext
            // — sätts upp av JwtAuthFilter på varje autentiserad request
            @AuthenticationPrincipal Staff author) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(ticketId, dto, author));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
    }
}