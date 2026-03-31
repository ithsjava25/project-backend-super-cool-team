package org.example.cyberwatch.config;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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

        staffRepository.saveAll(List.of(
                createStaff("19900101-0101", "Alice", "Andersson", "alice@cyberwatch.local", "070-1111111", Role.HR),
                createStaff("19880505-0505", "Bob", "Berg", "bob@cyberwatch.local", "070-2222222", Role.CEO),
                createStaff("19770707-0707", "Carla", "Carlsson", "carla@cyberwatch.local", "070-3333333", Role.CTO)
        ));
    }

    private Staff createStaff(String ssn, String firstName, String lastName, String email, String phone, Role role) {
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

