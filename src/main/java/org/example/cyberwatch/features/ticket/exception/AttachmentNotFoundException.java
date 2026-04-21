package org.example.cyberwatch.features.ticket.exception;

public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(Long id) {
        super("Bilaga med id " + id + " hittades inte");
    }
}