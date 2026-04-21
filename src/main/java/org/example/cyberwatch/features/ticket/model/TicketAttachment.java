package org.example.cyberwatch.features.ticket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    // fileUrl är borttagen – direkta S3-URL:er ska aldrig exponeras mot klienten.
    // Nedladdning sker via /api/tickets/{ticketId}/attachments/{id}/download
    // som genererar en tidsbegränsad presigned URL efter behörighetskontroll.

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
}