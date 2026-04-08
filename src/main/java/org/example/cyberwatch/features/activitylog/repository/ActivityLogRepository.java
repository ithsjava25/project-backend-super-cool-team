package org.example.cyberwatch.features.activitylog.repository;

import org.example.cyberwatch.features.activitylog.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByTicketIdOrderByTimestampAsc(Long ticketId);
}