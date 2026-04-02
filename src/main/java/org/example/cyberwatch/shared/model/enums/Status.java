package org.example.cyberwatch.shared.model.enums;

//Förslag: DRAFT -> SUBMITTED -> IN_PROGRESS -> RESOLVED -> CLOSED + REOPENED
public enum Status {
    DRAFT,
    SUBMITTED,
    IN_PROGRESS,
    WAITING_FOR_USER, // Används när man väntar på mer information etc.
    RESOLVED,
    CLOSED,
    REOPENED,
    PENDING,
    APPROVED
}
