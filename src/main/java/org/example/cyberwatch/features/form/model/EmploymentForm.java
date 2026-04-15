package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Could it be an idea that the EmployeeForm needs to be approved by an Manager, like a signature on a paper form?
// Then we could have a status field in the EmployeeForm with the following states:
//DRAFT -> SUBMITTED -> APPROVED -> COMPLETED -> REJECTED
@Getter
@Setter
@Entity
public class EmploymentForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Pattern(regexp = "^\\d{7,15}$", message = "Phone number must be between 7 and 15 digits e.g 0046722334455")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role cannot be null")
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Department cannot be null")
    private Department department;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status cannot be null")
    private ApprovalStatus status;

    @Column(name = "created_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdDate;

    @Column(name = "employed_s3_key")
    private String employedS3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hr_id", nullable = true)
    private Staff createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_management_id", nullable = true)
    private Staff approvedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public void setSocialSecurityNumber(String socialSecurityNumber) { this.socialSecurityNumber = socialSecurityNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getEmployedS3Key() { return employedS3Key; }
    public void setEmployedS3Key(String employedS3Key) { this.employedS3Key = employedS3Key; }
    public Staff getCreatedBy() { return createdBy; }
    public void setCreatedBy(Staff createdBy) { this.createdBy = createdBy; }
    public Staff getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Staff approvedBy) { this.approvedBy = approvedBy; }

    public EmploymentForm() {
    }

}
