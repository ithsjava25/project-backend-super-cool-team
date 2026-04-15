package org.example.cyberwatch.features.ticket.repository;

import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Ticket> findAll();

    List<Ticket> findByStatus(Status status);
    List<Ticket> findByAssignedToId(Long staffId);
    List<Ticket> findByCreatedById(Long staffId);

    /**
     * Dynamisk filtrering med valfria parametrar.
     * Varje parameter är valfri — om null ignoreras den i WHERE-satsen.
     * Använder JPQL med LEFT JOIN FETCH för att undvika N+1-queries.
     */
    @Query("""
            SELECT t FROM Ticket t
            LEFT JOIN FETCH t.createdBy
            LEFT JOIN FETCH t.assignedTo
            WHERE (:status IS NULL OR t.status = :status)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:issueType IS NULL OR t.issueType = :issueType)
            AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
            AND (:createdById IS NULL OR t.createdBy.id = :createdById)
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findByFilters(
            @Param("status") Status status,
            @Param("priority") Priority priority,
            @Param("issueType") IssueType issueType,
            @Param("assignedToId") Long assignedToId,
            @Param("createdById") Long createdById
    );
}