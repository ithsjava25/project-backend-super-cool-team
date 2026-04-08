package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.features.form.repository.ReportFormRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportFormService {
    private final ReportFormRepository reportFormRepository;

    public ReportFormService(ReportFormRepository reportFormRepository) {
        this.reportFormRepository = reportFormRepository;
    }

    //create report and insert in ticket

}
