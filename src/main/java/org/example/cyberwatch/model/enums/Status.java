package org.example.cyberwatch.model.enums;

//Förslag: DRAFT -> SUBMITTED -> IN_PROGRESS -> RESOLVED -> CLOSED + REOPENED
public enum Status {
    DRAFT,
    SUBMITTED,
    IN_PROGRESS,
    WAITING_FOR_USER, //
    RESOLVED,
    CLOSED,
    REOPENED
}
