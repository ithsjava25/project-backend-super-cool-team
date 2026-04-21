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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<EmploymentFormDTO> createEmploymentForm(
            @Valid @RequestBody CreateEmploymentDTO dto,
            @AuthenticationPrincipal Staff hrStaff) { // Direct injection

        EmploymentFormDTO createdForm = employmentFormService.createForm(dto, hrStaff);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdForm);
    }

    //Change returntype when emailservice is implemented
    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveForm(
            @PathVariable Long id,
            @AuthenticationPrincipal Staff manager) {
        return ResponseEntity.ok(employmentFormService.approveAndFinalizeEmployment(id, manager));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmploymentFormDTO> updateForm(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmploymentDTO dto,
            @AuthenticationPrincipal Staff hrStaff) {

        EmploymentFormDTO updatedForm = employmentFormService.updateFormBeforeApproval(id, dto, hrStaff);
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
            @AuthenticationPrincipal Staff manager) {

        String message = employmentFormService.rejectForm(id, manager);
        return ResponseEntity.ok(message);
    }

    // Delete a form
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(
            @PathVariable Long id,
            @AuthenticationPrincipal Staff staff) {

        employmentFormService.deleteForm(id, staff);
        return ResponseEntity.noContent().build();
    }

}
