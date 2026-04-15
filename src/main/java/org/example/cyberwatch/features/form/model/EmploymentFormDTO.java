package org.example.cyberwatch.features.form.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;

import java.time.LocalDateTime;

public class EmploymentFormDTO {

    private Long id;

    private String socialSecurityNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private Role role;

    private Department department;

    private ApprovalStatus status;

    private LocalDateTime createdDate;

    //Long when fetching the Id?
    private Long hrId;

    private Long approverManagementId;

    public EmploymentFormDTO() {}

    public EmploymentFormDTO(Long id, String socialSecurityNumber, String firstName, String lastName, String email, String phoneNumber, Role role, Department department, ApprovalStatus status, LocalDateTime createdDate, Long hrId, Long approverManagementId) {
        this.id = id;
        this.socialSecurityNumber = socialSecurityNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.department = department;
        this.status = status;
        this.createdDate = createdDate;
        this.hrId = hrId;
        this.approverManagementId = approverManagementId;
    }

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
    public Long getHrId() { return hrId; }
    public void setHrId(Long hrId) { this.hrId = hrId; }
    public Long getApproverManagementId() { return approverManagementId; }
    public void setApproverManagementId(Long approverManagementId) { this.approverManagementId = approverManagementId; }
}

