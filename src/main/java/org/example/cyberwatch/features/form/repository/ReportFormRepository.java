package org.example.cyberwatch.features.form.repository;

import org.example.cyberwatch.features.form.model.ReportForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportFormRepository extends JpaRepository<ReportForm, Long> {
}
