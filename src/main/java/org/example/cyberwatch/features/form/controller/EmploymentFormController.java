package org.example.cyberwatch.features.form.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.service.EmploymentFormService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
public class EmploymentFormController {

    private final EmploymentFormService employmentFormService;

    public EmploymentFormController(EmploymentFormService employmentFormService) {
        this.employmentFormService = employmentFormService;
    }

    @PostMapping("/employment")
    public ResponseEntity<EmploymentFormDTO> createEmploymentForm(@Valid @RequestBody CreateEmploymentDTO dto) {
        EmploymentFormDTO createdForm = employmentFormService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdForm);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<EmploymentFormDTO>> getPendingForms() {
        List<EmploymentFormDTO> pendingForms = employmentFormService.getPendingForms();
        return ResponseEntity.ok(pendingForms);
    }
}
