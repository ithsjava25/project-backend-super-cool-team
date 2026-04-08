package org.example.cyberwatch.features.staff.exception;

public class StaffNotFoundException extends RuntimeException {
    //Added extra method
    public StaffNotFoundException(Long id) {
        super("Staff with id " + id + " not found");
    }

    public StaffNotFoundException(String message) {
        super(message);
    }
}