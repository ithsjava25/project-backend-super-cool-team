package org.example.cyberwatch.features.staff.repository;

import org.example.cyberwatch.features.staff.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsBySocialSecurityNumber(String socialSecurityNumber);

    Optional<Staff> findByEmail(String email);
}

