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

    @OneToOne(optional = false)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    Staff staff;

    @Enumerated(EnumType.STRING)
    Department department;

    public Consultant() {
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
