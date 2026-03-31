package org.example.cyberwatch.features.ticket.model;

import jakarta.persistence.*;

@Entity
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;
    private String s3Key;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    public TicketAttachment() {
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getS3Key() {
        return s3Key;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }
}