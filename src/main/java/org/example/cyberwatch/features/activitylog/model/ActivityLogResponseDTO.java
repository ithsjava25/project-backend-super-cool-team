package org.example.cyberwatch.features.activitylog.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ActivityLogResponseDTO {

    private final Long id;
    private final Long ticketId;
    private final String performedBy;
    private final Long performedById;
    private final ActivityType activityType;
    private final String details;
    private final LocalDateTime timestamp;

    public ActivityLogResponseDTO(ActivityLog log) {
        this.id = log.getId();
        this.ticketId = log.getTicket().getId();
        this.performedBy = log.getPerformedBy().getFirstName() + " " + log.getPerformedBy().getLastName();
        this.performedById = log.getPerformedBy().getId();
        this.activityType = log.getActivityType();
        this.details = log.getDetails();
        this.timestamp = log.getTimestamp();
    }
}