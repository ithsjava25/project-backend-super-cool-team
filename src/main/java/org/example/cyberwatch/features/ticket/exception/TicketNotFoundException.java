package org.example.cyberwatch.features.ticket.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long id) {
        super("Ticket med id " + id + " hittades inte");
    }

    public TicketNotFoundException(String message) {
        super(message);
    }
}