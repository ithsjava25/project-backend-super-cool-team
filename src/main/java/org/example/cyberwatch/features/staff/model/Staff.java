package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.features.form.model.ReportForm;
import org.example.cyberwatch.features.ticket.model.Ticket;
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
    private Long id;

    @Column(name = "social_security_number", nullable = false, unique = true)
    @NotBlank(message = "Social security number cannot be blank")
    @Pattern(regexp = "\\d{6}-\\d{4}|\\d{8}-\\d{4}", message = "Social security number must be in format YYMMDD-NNNN or YYYYMMDD-NNNN")
    private String socialSecurityNumber;

    @Column(name = "first_name")
    @NotBlank(message = "First name cannot be blank")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Column(name = "last_name")
    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @Column(name = "phone_number")
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\d{7,15}$", message = "Phone number must be between 7 and 15 digits")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role cannot be null")
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Department cannot be null")
    private Department department;

    // An employee can have many reports
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReportForm> reportForms = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<EmploymentForm> createdForms;

    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Ticket> assignedTickets = new HashSet<>();

    public Staff() {

    }

}
