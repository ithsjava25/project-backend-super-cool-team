package org.example.cyberwatch.features.staff.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.UpdateStaffDTO;
import org.example.cyberwatch.features.staff.service.StaffService;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    //Hämta en staff
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<StaffDTO> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStaffById(id));
    }

    //updatera en staff
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<StaffDTO> updateStaff(@PathVariable Long id, @Valid @RequestBody UpdateStaffDTO dto) {
        return ResponseEntity.ok(service.updateStaff(id, dto));
    }

    //radera en staff
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        service.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    //visa en lista baserat på staff eller department, visar allt om inget filter anges
    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'CEO', 'CTO')")
    public ResponseEntity<List<StaffDTO>> getStaffByRoleOrDepartment(@RequestParam(required = false) Role role,
                                                                     @RequestParam(required = false) Department department) {
        return ResponseEntity.ok(service.getStaffByRoleOrDepartment(role, department));

    }
}
