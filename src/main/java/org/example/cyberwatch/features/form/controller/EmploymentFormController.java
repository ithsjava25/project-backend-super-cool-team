package org.example.cyberwatch.features.form.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.form.dto.CreateEmploymentDTO;
import org.example.cyberwatch.features.form.dto.EmploymentFormDTO;
import org.example.cyberwatch.features.form.dto.UpdateEmploymentDTO;
import org.example.cyberwatch.features.form.service.EmploymentFormService;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
public class EmploymentFormController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmploymentFormController.class);

    private final EmploymentFormService employmentFormService;

    public EmploymentFormController(EmploymentFormService employmentFormService) {
        this.employmentFormService = employmentFormService;
    }

    @PostMapping("/employment")
    public ResponseEntity<EmploymentFormDTO> createEmploymentForm(@Valid @RequestBody CreateEmploymentDTO dto,
                                                                  Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.example.cyberwatch.features.staff.model.Staff staff)) {
            throw new AccessDeniedException("Principal must be a Staff object");
        }
        logger.info("Creating employment form for HR staff: {}", staff.getEmail());
        EmploymentFormDTO createdForm = employmentFormService.createForm(dto, staff);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdForm);
    }

    //Change returntype when emailservice is implemented
    @PostMapping("/{id}/approve")
    //Change returntype when emailservice is implemented
    public ResponseEntity<String> approveForm(@PathVariable Long id, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.example.cyberwatch.features.staff.model.Staff staff)) {
            throw new AccessDeniedException("Principal must be a Staff object");
        }
        logger.info("Creating employment form with approval from staff: {}", staff.getEmail());
        return ResponseEntity.ok(
                employmentFormService.approveAndFinalizeEmployment(id, staff));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentFormDTO> updateForm(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmploymentDTO dto,
            Authentication auth) {
        Object principal = auth.getPrincipal();
        if (!(principal instanceof org.example.cyberwatch.features.staff.model.Staff staff)) {
            throw new AccessDeniedException("Principal must be a Staff object");
        }
        EmploymentFormDTO updatedForm = employmentFormService.updateFormBeforeApproval(id, dto, staff);
        return ResponseEntity.ok(updatedForm);
    }

    //Show list of pending forms
    @GetMapping
    public ResponseEntity<List<EmploymentFormDTO>> getForms(@RequestParam(required = false) ApprovalStatus status) {
        return ResponseEntity.ok(employmentFormService.getFormsByFilterApproval(status));
    }

    //Get a form by id
    @GetMapping("{id}")
    public ResponseEntity<EmploymentFormDTO> getFormById(@PathVariable Long id) {
        return ResponseEntity.ok(employmentFormService.getFormById(id));
    }

    // Reject a form with a reason
    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectForm(
            @PathVariable Long id,
            Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.example.cyberwatch.features.staff.model.Staff staff)) {
            throw new AccessDeniedException("Principal must be a Staff object");
        }

        String message = employmentFormService.rejectForm(id, staff);
        return ResponseEntity.ok(message);
    }

    // Delete a form
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(
            @PathVariable Long id,
            Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.example.cyberwatch.features.staff.model.Staff staff)) {
            throw new AccessDeniedException("Principal must be a Staff object");
        }
        employmentFormService.deleteForm(id, staff);
        return ResponseEntity.noContent().build();
    }

}
