package org.example.cyberwatch.features.activitylog.service;

import org.example.cyberwatch.features.activitylog.model.ActivityLog;
import org.example.cyberwatch.features.activitylog.model.ActivityLogResponseDTO;
import org.example.cyberwatch.features.activitylog.model.ActivityType;
import org.example.cyberwatch.features.activitylog.repository.ActivityLogRepository;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void logStatusChange(Ticket ticket, Staff performedBy, Status oldStatus, Status newStatus) {
        String details = oldStatus + " → " + newStatus;
        ActivityLog log = new ActivityLog(ticket, performedBy, ActivityType.STATUS_CHANGED, details);
        activityLogRepository.save(log);
    }

    public void logFileUpload(Ticket ticket, Staff uploadedBy, String fileName) {
        String details = "Fil uppladdad: " + fileName;
        ActivityLog log = new ActivityLog(ticket, uploadedBy, ActivityType.FILE_UPLOADED, details);
        activityLogRepository.save(log);
    }

    public void logComment(Ticket ticket, Staff performedBy, String commentText) {
        ActivityLog log = new ActivityLog(ticket, performedBy, ActivityType.COMMENT_ADDED, commentText);
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponseDTO> getLogsForTicket(Long ticketId) {
        return activityLogRepository.findByTicketIdOrderByTimestampAsc(ticketId)
                .stream()
                .map(ActivityLogResponseDTO::new)
                .toList();
    }
}