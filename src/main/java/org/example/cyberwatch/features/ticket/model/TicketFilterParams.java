package org.example.cyberwatch.features.ticket.model;

import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

public class TicketFilterParams {
    private Status status;
    private Priority priority;
    private IssueType issueType;
    private Long assignedStaffId;
    private Long createdById;
    private String search;

    public TicketFilterParams() {}

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }
    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public boolean isEmpty() {
        return status == null && priority == null && issueType == null
                && assignedStaffId == null && createdById == null && (search == null || search.isBlank());
    }
}