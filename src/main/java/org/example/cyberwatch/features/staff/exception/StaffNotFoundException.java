package org.example.cyberwatch.features.staff.exception;

public class StaffNotFoundException extends RuntimeException {

    public StaffNotFoundException(String message) {
        super(message);
    }
}