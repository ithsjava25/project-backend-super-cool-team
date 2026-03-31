package org.example.cyberwatch.config;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final StaffRepository staffRepository;

    public DataInitializer(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }

    }

    private Staff createStaff(String ssn, String firstName, String lastName, String email, String phone, String role) {
        Staff staff = new Staff();
        staff.setSocialSecurityNumber(ssn);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmail(email);
        staff.setPhoneNumber(phone);
        staff.setRole(role);
        return staff;
    }
}

