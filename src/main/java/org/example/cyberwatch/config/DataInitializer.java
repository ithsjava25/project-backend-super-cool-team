package org.example.cyberwatch.config;

import org.example.cyberwatch.config.security.EncryptionService;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final EmploymentFormRepository employmentFormRepository;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(StaffRepository staffRepository, EmploymentFormRepository employmentFormRepository,
                           EncryptionService encryptionService, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.employmentFormRepository = employmentFormRepository;
        this.encryptionService = encryptionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }

        // Skapa admin-användare
        List<Staff> admins = staffRepository.saveAll(List.of(
                createStaff("19900101-0101", "Eric", "Thilen", "ericthilen2003@gmail.com", "0701111111", Role.ADMIN, Department.BACKEND, "testPass123", "ONLINE"),
                createStaff("19900505-0505", "Caroline", "Nordbrandt", "nordbrandtcaroline@gmail.com", "0702222222", Role.ADMIN, Department.DEVOPS, "testPass1234", "ONLINE"),
                createStaff("19900707-0707", "Alice", "Wersen", "alicewersen@hotmail.com", "0703333333", Role.ADMIN, Department.HR, "testPass12345", "OFFLINE"),
                createStaff("19900909-0909", "Younes", "Lamia", "younescool94@gmail.com", "0704444444", Role.ADMIN, Department.FRONTEND, "testPass1234", "OFFLINE")
        ));

        // Fler dummies för att testa filter och scroll
        staffRepository.saveAll(List.of(
                createStaff("19850212-1212", "Johan", "Andersson", "johan.a@cyberwatch.se", "0705555555", Role.CTO, Department.BACKEND, "pass123", "BUSY"),
                createStaff("19920314-3412", "Sofia", "Lindgren", "sofia.l@cyberwatch.se", "0706666666", Role.HR, Department.HR, "pass123", "OFFLINE"),
                createStaff("19881120-5678", "Marcus", "Ek", "marcus.ek@cyberwatch.se", "0707777777", Role.CEO, Department.MANAGEMENT, "pass123", "AWAY"),
                createStaff("19950606-9999", "Linnea", "Berg", "linnea.b@cyberwatch.se", "0708888888", Role.CONSULTANT, Department.FRONTEND, "pass123", "OFFLINE"),
                createStaff("19910825-4433", "Niklas", "Sjöberg", "niklas.s@cyberwatch.se", "0709999999", Role.CONSULTANT, Department.DEVOPS, "pass123", "AWAY"),
                createStaff("19931201-1010", "Elena", "Popova", "elena.p@cyberwatch.se", "0701010101", Role.CONSULTANT, Department.BACKEND, "pass123", "OFFLINE"),
                createStaff("19870412-2233", "Mikael", "Vesterberg", "mikael.v@cyberwatch.se", "0721112233", Role.PROJECT_MANAGER, Department.DEVOPS, "testPass123", "BUSY"),
                createStaff("19941030-4455", "Sara", "Lundin", "sara.l@cyberwatch.se", "0734445566", Role.CONSULTANT, Department.BACKEND, "testPass123", "BUSY"),
                createStaff("19910228-6677", "David", "Holm", "david.h@cyberwatch.se", "0767778899", Role.HR, Department.HR, "testPass123", "ONLINE"),
                createStaff("19890515-8899", "Emma", "Sjölin", "emma.s@cyberwatch.se", "0708889900", Role.PROJECT_MANAGER, Department.FRONTEND, "testPass123", "ONLINE"),
                createStaff("19960820-1122", "Lucas", "Karlsson", "lucas.k@cyberwatch.se", "0791112233", Role.CONSULTANT, Department.BACKEND, "testPass123", "OFFLINE")
        ));

        // Skapa 3 pending employment forms
        Staff hrStaff = admins.get(0); // Eric (Admin/HR)
        employmentFormRepository.saveAll(List.of(
                createEmploymentForm("19800315-2525", "Peter", "Bergström", "peter.bergstrom@cyberwatch.se", "0715151515", Role.CONSULTANT, Department.BACKEND, hrStaff),
                createEmploymentForm("19870723-3636", "Victoria", "Ström", "victoria.strom@cyberwatch.se", "0726262626", Role.PROJECT_MANAGER, Department.FRONTEND, hrStaff),
                createEmploymentForm("19920411-4747", "Martin", "Nordin", "martin.nordin@cyberwatch.se", "0737373737", Role.CONSULTANT, Department.DEVOPS, hrStaff)
        ));
    }

    private Staff createStaff(String ssn, String firstName, String lastName, String email, String phone,
                              Role role, Department department, String rawPassword, String status) {
        Staff staff = new Staff();
        staff.setSocialSecurityNumber(encryptionService.encrypt(ssn));
        staff.setSsnHash(encryptionService.hmac(ssn));
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setEmail(email);
        staff.setPhoneNumber(phone);
        staff.setRole(role);
        staff.setDepartment(department);
        staff.setStatus(status);
        staff.setPassword(passwordEncoder.encode(rawPassword));
        return staff;
    }

    private EmploymentForm createEmploymentForm(String ssn, String firstName, String lastName, String email,
                                                String phone, Role role, Department department, Staff createdBy) {
        EmploymentForm form = new EmploymentForm();
        form.setSocialSecurityNumber(encryptionService.encrypt(ssn));
        form.setSsnHash(encryptionService.hmac(ssn));
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);
        form.setPhoneNumber(phone);
        form.setRole(role);
        form.setDepartment(department);
        form.setStatus(ApprovalStatus.PENDING);
        form.setCreatedDate(LocalDateTime.now());
        form.setCreatedBy(createdBy);
        return form;
    }
}

