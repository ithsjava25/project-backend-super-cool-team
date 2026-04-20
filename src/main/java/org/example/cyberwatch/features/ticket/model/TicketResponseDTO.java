package org.example.cyberwatch.features.ticket.model;

import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TicketResponseDTO {
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
    private List<StaffSummary> assignedStaff = new ArrayList<>();

    // Bilagor returneras med en relativ nedladdningslänk istället för direkt S3-URL.
    // Frontend anropar downloadUrl för att få en tidsbegränsad presigned URL via backend.
    private List<AttachmentSummary> attachments = new ArrayList<>();

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class AttachmentSummary {
        private final Long id;
        private final String fileName;
        private final String downloadUrl;

        public AttachmentSummary(Long id, String fileName, Long ticketId) {
            this.id = id;
            this.fileName = fileName;
            // Relativ URL – backend kontrollerar behörighet och redirectar till presigned URL
            this.downloadUrl = "/api/tickets/" + ticketId + "/attachments/" + id + "/download";
        }
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

        if (ticket.getAssignedStaff() != null) {
            for (Staff staff : ticket.getAssignedStaff()) {
                dto.assignedStaff.add(new StaffSummary(
                        staff.getId(),
                        staff.getFirstName() + " " + staff.getLastName(),
                        staff.getEmail()
                ));
            }
        }

        if (ticket.getAttachments() != null) {
            for (TicketAttachment attachment : ticket.getAttachments()) {
                dto.attachments.add(new AttachmentSummary(
                        attachment.getId(),
                        attachment.getFileName(),
                        ticket.getId()
                ));
            }
        }

        return dto;
    }
}