package org.example.cyberwatch.features.form.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.cyberwatch.features.form.model.EmploymentForm;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentFormRepository extends JpaRepository<EmploymentForm, Long> {
    List<EmploymentForm> findByStatus(ApprovalStatus status);

    boolean existsBySocialSecurityNumber(@NotBlank(message = "Social security number cannot be blank") @Pattern(regexp = "\\d{6}-\\d{4}|\\d{8}-\\d{4}", message = "Social security number must be in format YYMMDD-NNNN or YYYYMMDD-NNNN") String socialSecurityNumber);
}
