package org.example.cyberwatch.config;

import org.example.cyberwatch.config.security.EncryptionService;
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
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(StaffRepository staffRepository, EncryptionService encryptionService, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.encryptionService = encryptionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }

        staffRepository.saveAll(List.of(
                createStaff("19900101-0101", "Eric", "Thilen", "ericthilen2003@gmail.com", "0701111111", Role.ADMIN, Department.BACKEND, "testPass123"),
                createStaff("19900505-0505", "Caroline", "Nordbrandt", "nordbrandtcaroline@gmail.com", "0702222222", Role.ADMIN, Department.DEVOPS, "testPass1234"),
                createStaff("19900707-0707", "Alice", "Wersen", "alicewersen@hotmail.com", "0703333333", Role.ADMIN, Department.HR, "testPass12345"),
                createStaff("19900909-0909", "Younes", "Lamia", "younescool94@gmail.com", "0704444444", Role.ADMIN, Department.FRONTEND, "testPass1234")
        ));
    }

    private Staff createStaff(String ssn, String firstName, String lastName, String email, String phone, Role role, Department department, String rawPassword) {
        Staff staff = new Staff();
        staff.setSocialSecurityNumber(encryptionService.encrypt(ssn));
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmail(email);
        staff.setPhoneNumber(phone);
        staff.setRole(role);
        staff.setDepartment(department);
        staff.setPassword(passwordEncoder.encode(rawPassword));
        return staff;
    }
}

