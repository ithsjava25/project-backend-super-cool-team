package org.example.cyberwatch.model.entitys.staff;

import jakarta.persistence.*;
import org.example.cyberwatch.model.enums.Department;

// Represents an HR administrator role profile linked 1:1 to Staff.
// Can create EmploymentForm for new hires and manage case assignments (HR-only privilege).
@Entity
public class HR {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    Staff staff;

    @Enumerated(EnumType.STRING)
    Department department;

    public HR() {
    }

}
