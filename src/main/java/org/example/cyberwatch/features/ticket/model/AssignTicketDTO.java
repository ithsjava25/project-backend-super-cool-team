package org.example.cyberwatch.features.ticket.model;

import java.util.List;

public class AssignTicketDTO {
    private List<Long> staffIds;

    public AssignTicketDTO() {}

    public List<Long> getStaffIds() { return staffIds; }
    public void setStaffIds(List<Long> staffIds) { this.staffIds = staffIds; }
}