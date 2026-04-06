package org.example.cyberwatch.features.form.repository;

import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentFormRepository extends JpaRepository<EmploymentForm, Long> {
    List<EmploymentForm> findByStatus(ApprovalStatus status);

    boolean existsBySocialSecurityNumber(String socialSecurityNumber);
}
