package org.example.cyberwatch.features.staff.repository;

import org.example.cyberwatch.features.ticket.model.entitys.staff.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
}

