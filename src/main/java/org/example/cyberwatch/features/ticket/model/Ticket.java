package org.example.cyberwatch.features.ticket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.SUBMITTED;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Staff createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Staff assignedTo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void advanceStatus() {
        this.status = switch (this.status) {
            case DRAFT            -> Status.SUBMITTED;
            case SUBMITTED        -> Status.IN_PROGRESS;
            case IN_PROGRESS      -> Status.RESOLVED;
            case WAITING_FOR_USER -> Status.IN_PROGRESS;
            case RESOLVED         -> Status.CLOSED;
            case REOPENED         -> Status.IN_PROGRESS;
            case CLOSED           -> throw new IllegalStateException(
                    "Ärendet är redan stängt. Återöppna det först.");
        };
    }

    public void reopen() {
        if (this.status != Status.CLOSED && this.status != Status.RESOLVED) {
            throw new IllegalStateException("Kan bara återöppna ett stängt eller löst ärende.");
        }
        this.status = Status.REOPENED;
    }
}