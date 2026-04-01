package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.shared.model.enums.Department;

import java.util.Set;

// Represents a manager/supervisor role, role profile linked 1:1 to a Staff entity.
// Can approve and oversee cases assigned to their department.
@Getter
@Setter
@Entity
public class Management {

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

    @OneToMany(mappedBy = "approvedBy")
    private Set<EmploymentForm> approvedForms;

    public Management() {
    }

}
