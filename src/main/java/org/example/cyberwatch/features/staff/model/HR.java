package org.example.cyberwatch.features.staff.model;

import jakarta.persistence.*;
import org.example.cyberwatch.shared.model.enums.Department;

// Represents an HR administrator role profile linked 1:1 to Staff.
// Can create EmploymentForm for new hires and manage case assignments (HR-only privilege).
@Entity
public class HR {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    Staff staff;

    @Enumerated(EnumType.STRING)
    Department department;

    public HR() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}
