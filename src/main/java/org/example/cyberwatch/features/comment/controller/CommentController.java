package org.example.cyberwatch.features.comment.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.comment.model.CommentDTO;
import org.example.cyberwatch.features.comment.model.CommentResponseDTO;
import org.example.cyberwatch.features.comment.service.CommentService;
import org.example.cyberwatch.features.staff.model.Staff;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @PostMapping
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CommentDTO dto,
            @AuthenticationPrincipal Staff author) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(ticketId, dto, author));
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
    }
}