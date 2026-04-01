package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Department;

// Represents a consultant/regular employee role profile linked 1:1 to Staff.
// Can create ReportForm, add comments and upload files to assigned cases.
@Getter
@Setter
@Entity
public class Consultant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    @NotNull(message = "Staff cannot be null")
    Staff staff;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Department cannot be null")
    Department department;

    public Consultant() {
    }

}
