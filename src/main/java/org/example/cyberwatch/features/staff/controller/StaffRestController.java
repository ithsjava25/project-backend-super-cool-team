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

    @GetMapping("/me")
    //skapa en ny metod i service
    public ResponseEntity<StaffDTO> getCurrentUser(@AuthenticationPrincipal Staff staff) {
        if (staff == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(staffService.getUserStaff(staff));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<StaffDTO> updateMyStatus(@AuthenticationPrincipal Staff staff,
                                                   @RequestParam String status) {
        if (staff == null) {
            return ResponseEntity.status(401).build();
        }

        if (!List.of("ONLINE", "BUSY", "AWAY", "OFFLINE").contains(status)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(staffService.updateStatus(staff.getId(), status, staff));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDTO> getStaffById(@PathVariable Long id,
                                                 @AuthenticationPrincipal Staff user) {

        return ResponseEntity.ok(staffService.getStaffById(id, user));

    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> updateStaff(@PathVariable Long id, @Valid @RequestBody UpdateStaffDTO dto, @AuthenticationPrincipal Staff staff) {
        return ResponseEntity.ok(staffService.updateStaff(id, dto, staff));
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
        return ResponseEntity.ok(staffService.getStaffByRoleOrDepartment(role, department, user));
    }
}