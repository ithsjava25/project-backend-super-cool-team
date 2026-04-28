package org.example.cyberwatch.features.staff.repository;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsBySocialSecurityNumber(String socialSecurityNumber);

    boolean existsBySsnHash(String ssnHash);

    Optional<Staff> findByEmail(String email);

    List<Staff> findByRole(Role role);

    List<Staff> findByDepartment(Department department);
}

