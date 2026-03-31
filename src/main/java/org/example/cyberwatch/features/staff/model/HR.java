package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Department;

// Represents an HR administrator role profile linked 1:1 to Staff.
// Can create EmploymentForm for new hires and manage case assignments (HR-only privilege).
@Getter
@Setter
@Entity
public class HR {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    @NotNull(message = "Staff cannot be null")
    Staff staff;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Staff cannot be null")
    Department department;

    public HR() {
    }

}
