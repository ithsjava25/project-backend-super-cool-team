package org.example.cyberwatch.features.staff.controller;

import lombok.RequiredArgsConstructor;
import org.example.cyberwatch.features.staff.model.StaffResponseDTO;
import org.example.cyberwatch.features.staff.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffRestController {

    private final StaffService staffService;

    public StaffRestController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }
}
