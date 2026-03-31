package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.form.model.ReportForm;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;

import java.util.HashSet;
import java.util.Set;

// Represents an employee in the system, base entity for all staff with personal information. Linked 1:1 to HR/Management/Consultant roles.
//Differs from Employee form wich is the process/form HR uses to create a new employee.
@Getter
@Setter
@Entity
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    Long id;

    @Column(name = "social_security_number", nullable = false, unique = true)
    String socialSecurityNumber;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Email(message = "Email should be valid")
    String email;

    @Column(name = "phone_number")
    String phoneNumber;

    @Enumerated(EnumType.STRING)
    Role role;

    @Enumerated(EnumType.STRING)
    Department department;

    // An employee can have many reports
    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReportForm> reportForms = new HashSet<>();

    public Staff() {

    }

}
