package org.example.cyberwatch.features.ticket.model;

import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
public class TicketResponseDTO {
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public StaffSummary getCreatedBy() { return createdBy; }
    public void setCreatedBy(StaffSummary createdBy) { this.createdBy = createdBy; }
    public StaffSummary getAssignedTo() { return assignedTo; }
    public void setAssignedTo(StaffSummary assignedTo) { this.assignedTo = assignedTo; }

    private Long id;
    private String ticketCode;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private IssueType issueType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private StaffSummary createdBy;
    private StaffSummary assignedTo;

    @Getter
    public static class StaffSummary {
        private final Long id;
        private final String fullName;
        private final String email;

        public StaffSummary(Long id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
    }

    public static TicketResponseDTO from(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.id = ticket.getId();
        dto.ticketCode = ticket.getTicketCode();
        dto.title = ticket.getTitle();
        dto.description = ticket.getDescription();
        dto.status = ticket.getStatus();
        dto.priority = ticket.getPriority();
        dto.issueType = ticket.getIssueType();
        dto.createdAt = ticket.getCreatedAt();
        dto.updatedAt = ticket.getUpdatedAt();

        if (ticket.getCreatedBy() != null) {
            Staff c = ticket.getCreatedBy();
            dto.createdBy = new StaffSummary(
                    c.getId(),
                    c.getFirstName() + " " + c.getLastName(),
                    c.getEmail()
            );
        }

        if (ticket.getAssignedTo() != null) {
            Staff a = ticket.getAssignedTo();
            dto.assignedTo = new StaffSummary(
                    a.getId(),
                    a.getFirstName() + " " + a.getLastName(),
                    a.getEmail()
            );
        }

        return dto;
    }
}