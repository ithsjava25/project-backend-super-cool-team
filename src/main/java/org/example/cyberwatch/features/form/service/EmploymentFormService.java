package org.example.cyberwatch.features.form.service;

import org.example.cyberwatch.features.form.repository.EmploymentFormRepository;
import org.springframework.stereotype.Service;

@Service
public class EmploymentFormService {

    private final EmploymentFormRepository employmentFormRepository;

    public EmploymentFormService(EmploymentFormRepository employmentFormRepository) {
        this.employmentFormRepository = employmentFormRepository;
    }

    //Create employment & insert in staff.java

    //view
    //show all employmentforms with status waiting for approval

    //delete
    //When approved by management delete
}
