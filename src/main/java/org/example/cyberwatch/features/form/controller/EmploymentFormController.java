package org.example.cyberwatch.features.form.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.form.model.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.model.EmploymentFormDTO;
import org.example.cyberwatch.features.form.model.UpdateEmploymentDTO;
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

    @PostMapping(value = "/employment")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<EmploymentFormDTO> createEmploymentForm(@Valid @RequestBody CreateEmploymentDTO dto,
                                                                  Authentication authentication) {
        String loggedInHr = authentication.getName(); // Assuming this returns the HR staff's identifier
        EmploymentFormDTO createdForm = employmentFormService.createForm(dto, loggedInHr);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdForm);
    }


    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('CEO') or hasRole('CTO')")
    //Change returntype when emailservice is implemented
    public ResponseEntity<String> approveForm(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(
                employmentFormService.approveAndFinalizeEmployment(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<EmploymentFormDTO> updateForm(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmploymentDTO dto,
            Authentication auth) {

        EmploymentFormDTO updatedForm = employmentFormService.updateFormBeforeApproval(id, dto, auth.getName());
        return ResponseEntity.ok(updatedForm);
    }

    //Show list of pending forms
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<List<EmploymentFormDTO>> getPendingForms() {
        List<EmploymentFormDTO> pendingForms = employmentFormService.getPendingForms();
        return ResponseEntity.ok(pendingForms);
    }

    //Show list of approved forms
    @GetMapping("/approved")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<EmploymentFormDTO>> getApprovedForms() {
        List<EmploymentFormDTO> approved = employmentFormService.getApprovedForms();
        return ResponseEntity.ok(approved);
    }

    //Get a form by id
    @GetMapping("{id}")
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<EmploymentFormDTO> getFormById(@PathVariable Long id) {
        return ResponseEntity.ok(employmentFormService.getFormById(id));
    }

    // Reject a form with a reason
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('CEO') or hasRole('CTO')")
    public ResponseEntity<String> rejectForm(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {

        String message = employmentFormService.rejectForm(id, reason, authentication.getName());
        return ResponseEntity.ok(message);
    }

    // Delete a form
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<Void> deleteForm(
            @PathVariable Long id,
            Authentication authentication) {

        employmentFormService.deleteForm(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
