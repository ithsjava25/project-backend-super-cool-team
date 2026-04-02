package org.example.cyberwatch.features.comment.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentResponseDTO {

    private final Long id;
    private final Long ticketId;
    private final Long authorId;
    private final String authorName;
    private final String text;
    private final LocalDateTime createdAt;

    public CommentResponseDTO(Comment comment) {
        this.id = comment.getId();
        this.ticketId = comment.getTicket().getId();
        this.authorId = comment.getAuthor().getId();
        this.authorName = comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName();
        this.text = comment.getText();
        this.createdAt = comment.getCreatedAt();
    }
}