package org.example.cyberwatch.features.form.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.service.EmploymentFormService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<EmploymentFormDTO> createEmploymentForm(@Valid @RequestBody CreateEmploymentDTO dto, Authentication authentication) {
        String loggedInHr = authentication.getName(); // Assuming this returns the HR staff's identifier
        EmploymentFormDTO createdForm = employmentFormService.createForm(dto, loggedInHr);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdForm);
    }


    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('CEO' || 'CTO')")
    public ResponseEntity<Void> approveForm(@PathVariable Long id) {
        employmentFormService.approveAndFinalizeEmployment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR'||'CEO' || 'CTO')")
    public ResponseEntity<List<EmploymentFormDTO>> getPendingForms() {
        List<EmploymentFormDTO> pendingForms = employmentFormService.getPendingForms();
        return ResponseEntity.ok(pendingForms);
    }
}
