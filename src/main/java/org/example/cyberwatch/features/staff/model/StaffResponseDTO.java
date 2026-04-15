package org.example.cyberwatch.features.staff.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String role;
    private String department;

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
