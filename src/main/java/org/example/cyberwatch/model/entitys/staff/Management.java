package org.example.cyberwatch.model.entitys.staff;

import jakarta.persistence.*;
import org.example.cyberwatch.model.enums.Department;

// Represents a manager/supervisor role, role profile linked 1:1 to a Staff entity.
// Can approve and oversee cases assigned to their department.
@Entity
public class Management {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne
    Staff staff;

    @Enumerated(EnumType.STRING)
    Department department;

    public Management() {
    }

}
