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

    private Long id;
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
    }

    public static TicketResponseDTO from(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.id = ticket.getId();
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