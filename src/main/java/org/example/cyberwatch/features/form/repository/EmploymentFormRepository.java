package org.example.cyberwatch.features.form.repository;

import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentFormRepository extends JpaRepository<EmploymentForm, Long> {
    List<EmploymentForm> findByStatus(Status status);
}
