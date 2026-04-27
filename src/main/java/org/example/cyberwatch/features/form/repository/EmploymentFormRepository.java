package org.example.cyberwatch.features.form.repository;

import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmploymentFormRepository extends JpaRepository<EmploymentForm, Long> {
    List<EmploymentForm> findByStatus(ApprovalStatus status);

    boolean existsBySsnHash(String ssnHash);
    boolean existsBySocialSecurityNumber(String socialSecurityNumber);
}
