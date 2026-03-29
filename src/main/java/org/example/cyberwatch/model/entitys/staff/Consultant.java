package org.example.cyberwatch.model.entitys.staff;

import jakarta.persistence.*;
import org.example.cyberwatch.model.enums.Department;

// Represents a consultant/regular employee role profile linked 1:1 to Staff.
// Can create ReportForm, add comments and upload files to assigned cases.
@Entity
public class Consultant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    Staff staff;

    @Enumerated(EnumType.STRING)
    Department department;

    public Consultant() {
    }

}
