package org.example.cyberwatch.features.activitylog.controller;

import org.example.cyberwatch.features.activitylog.model.ActivityLogResponseDTO;
import org.example.cyberwatch.features.activitylog.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @PreAuthorize("@ticketSecurity.canAccess(authentication, #ticketId)")
    @GetMapping
    public ResponseEntity<List<ActivityLogResponseDTO>> getLogs(@PathVariable Long ticketId) {
        return ResponseEntity.ok(activityLogService.getLogsForTicket(ticketId));
    }
}