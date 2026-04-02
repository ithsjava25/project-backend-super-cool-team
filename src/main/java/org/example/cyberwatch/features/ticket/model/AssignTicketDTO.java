package org.example.cyberwatch.features.ticket.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AssignTicketDTO {

    @NotNull
    @Positive
    private Long staffId;

    public AssignTicketDTO() {
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
}