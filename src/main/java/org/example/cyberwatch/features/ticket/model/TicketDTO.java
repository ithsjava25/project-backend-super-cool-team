package org.example.cyberwatch.features.ticket.model;

import org.example.cyberwatch.shared.model.enums.Priority;

public class TicketDTO {

    private String title;
    private String description;
    private Priority priority;

    public TicketDTO() {
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}