package org.example.cyberwatch.features.ticket.model;

import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

/**
 * Samlar alla valfria filterparametrar för ticket-sökning.
 * Alla fält är null som default — om ett fält är null ignoreras det i filtreringen.
 * Används som @ModelAttribute i TicketController så att Spring automatiskt
 * mappar query params till objektets fält:
 * GET /api/tickets?status=IN_PROGRESS&priority=HIGH
 */
@Getter
@Setter
public class TicketFilterParams {

    private Status status;
    private Priority priority;
    private IssueType issueType;
    private Long assignedToId;
    private Long createdById;

    // Returnerar true om inga filter är satta — används för att skippa onödig filtrering
    public boolean isEmpty() {
        return status == null && priority == null && issueType == null
                && assignedToId == null && createdById == null;
    }
}