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
// Differs from Employee form which is the process/form HR uses to create a new employee.
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

    @Column(name = "email", unique = true, nullable = false)
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

    @Column(name = "password")
    private String password;

    @Column(name = "employed_s3_key")
    private String employedS3Key;

    public String getEmployedS3Key() { return employedS3Key; }
    public void setEmployedS3Key(String employedS3Key) { this.employedS3Key = employedS3Key; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public void setSocialSecurityNumber(String socialSecurityNumber) { this.socialSecurityNumber = socialSecurityNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}