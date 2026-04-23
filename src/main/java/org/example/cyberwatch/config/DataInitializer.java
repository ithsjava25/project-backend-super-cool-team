package org.example.cyberwatch.config;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }

        staffRepository.saveAll(List.of(
                createStaff("19900101-0101", "Alice", "Andersson", "alice@cyberwatch.local", "0701111111", Role.HR, Department.BACKEND, "testPass123"),
                createStaff("19880505-0505", "Bob", "Berg", "bob@cyberwatch.local", "0702222222", Role.CEO, Department.DEVOPS, "testPass1234"),
                createStaff("19770707-0707", "Carla", "Carlsson", "carla@cyberwatch.local", "0703333333", Role.CTO, Department.HR, "testPass12345")
        ));
    }

    private Staff createStaff(String ssn, String firstName, String lastName, String email, String phone, Role role, Department department, String rawPassword) {
        Staff staff = new Staff();
        staff.setSocialSecurityNumber(ssn);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmail(email);
        staff.setPhoneNumber(phone);
        staff.setRole(role);
        staff.setDepartment(department);
        staff.setStatus("OFFLINE");
        staff.setPassword(passwordEncoder.encode(rawPassword));
        return staff;
    }
}

