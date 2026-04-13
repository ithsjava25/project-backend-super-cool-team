package org.example.cyberwatch.features.staff.controller;

import org.example.cyberwatch.features.staff.model.StaffDTO;
import org.example.cyberwatch.features.staff.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    //Hämta en staff
    @GetMapping("{id}")
    public ResponseEntity<StaffDTO> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStaffById(id));
    }
    //updatera en staff
    //radera en staff
    //visa en lista av staff
    //visa en lista baserat på staff eller department
}
