package org.example.cyberwatch.features.ticket.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException (String message) {
        super(message);
    }
}
