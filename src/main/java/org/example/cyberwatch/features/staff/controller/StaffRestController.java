package org.example.cyberwatch.features.staff.controller;

import jakarta.validation.Valid;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.model.UpdateStaffDTO;
import org.example.cyberwatch.features.staff.service.StaffService;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffRestController {

    private final StaffService staffService;

    public StaffRestController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDTO> getStaffById(@PathVariable Long id,
                                                 @AuthenticationPrincipal Staff user) {

        return ResponseEntity.ok(staffService.getStaffById(id, user.getRole()));

    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> updateStaff(@PathVariable Long id, @Valid @RequestBody UpdateStaffDTO dto) {
        return ResponseEntity.ok(staffService.updateStaff(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<StaffDTO>> getStaffByRoleOrDepartment(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Department department,
            @AuthenticationPrincipal Staff user) {
        return ResponseEntity.ok(staffService.getStaffByRoleOrDepartment(role, department, user.getRole()));
    }

   /* @GetMapping
    public ResponseEntity<List<StaffDTO>> getAllStaff() {
        return ResponseEntity.ok(staffService.getStaffByRoleOrDepartment(null, null));
    }

    */
}
