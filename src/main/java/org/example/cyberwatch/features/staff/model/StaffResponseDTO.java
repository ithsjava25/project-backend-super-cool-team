package org.example.cyberwatch.features.staff.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class StaffResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String role;
    private String department;

    public StaffResponseDTO() {}

    public StaffResponseDTO(Long id, String firstName, String lastName, String fullName, String email, String role, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.department = department;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public static StaffResponseDTO from(Staff staff) {
        return new StaffResponseDTO(
                staff.getId(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getFirstName() + " " + staff.getLastName(),
                staff.getEmail(),
                staff.getRole() != null ? staff.getRole().name() : null,
                staff.getDepartment() != null ? staff.getDepartment().name() : null
        );
    }
}
