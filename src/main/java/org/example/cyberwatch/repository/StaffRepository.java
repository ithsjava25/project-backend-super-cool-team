package org.example.cyberwatch.repository;

import org.example.cyberwatch.model.entitys.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
}

