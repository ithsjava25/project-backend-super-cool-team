package org.example.cyberwatch.features.staff.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;

// Data Transfer Object for Staff - used for API requests/responses
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private Long id;
    private String socialSecurityNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private Department department;
}