package org.example.cyberwatch.features.ticket.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;

public class TicketDTO {

    @NotBlank(message = "Titel får inte vara tom")
    @Size(min = 5, max = 100, message = "Titel måste vara mellan 5-100 tecken!")
    private String title;

    @NotBlank(message = "Beskrivning får inte vara tom")
    @Size(max = 2000, message = "Beskrivning kan max vara 2000 tecken")
    private String description;

    @NotNull(message = "Prioritet måste anges!")
    private Priority priority;

    @NotNull(message = "Ärendetyp måste anges!")
    private IssueType issueType;

    @NotNull(message = "createdById måste anges")
    private Long createdById;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
}
