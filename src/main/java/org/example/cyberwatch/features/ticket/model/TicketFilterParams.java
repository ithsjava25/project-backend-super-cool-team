package org.example.cyberwatch.features.ticket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

@Getter
@Setter
@NoArgsConstructor
public class TicketFilterParams {
    private Status status;
    private Priority priority;
    private IssueType issueType;
    private Long assignedStaffId;
    private Long createdById;
    private String search;


    public boolean isEmpty() {
        return status == null && priority == null && issueType == null
                && assignedStaffId == null && createdById == null && (search == null || search.isBlank());
    }
}