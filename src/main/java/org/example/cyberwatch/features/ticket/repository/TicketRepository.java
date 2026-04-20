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
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedStaff"})
    List<Ticket> findAll();

    List<Ticket> findByStatus(Status status);

    @Query("SELECT t FROM Ticket t JOIN t.assignedStaff s WHERE s.id = :staffId")
    List<Ticket> findByAssignedStaffId(@Param("staffId") Long staffId);

    List<Ticket> findByCreatedById(Long staffId);

    /**
     * Dynamisk filtrering med valfria parametrar.
     * Varje parameter är valfri — om null ignoreras den i WHERE-satsen.
     * Använder JPQL med LEFT JOIN FETCH för att undvika N+1-queries.
     *
     * viewerId styr åtkomstkontroll per rad:
     * - null → ADMIN, ser alla ärenden
     * - satt  → användaren ser bara ärenden där de är skapare eller tilldelad handläggare
     *
     * EXISTS-subqueryn används för att undvika konflikter med LEFT JOIN s
     * som används för assignedToId-filtret.
     */
    @Query("""
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN FETCH t.createdBy
            LEFT JOIN FETCH t.assignedStaff
            LEFT JOIN t.assignedStaff s
            WHERE (:status IS NULL OR t.status = :status)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:issueType IS NULL OR t.issueType = :issueType)
            AND (:assignedToId IS NULL OR s.id = :assignedToId)
            AND (:createdById IS NULL OR t.createdBy.id = :createdById)
            AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:viewerId IS NULL
                OR t.createdBy.id = :viewerId
                OR EXISTS (SELECT ms FROM t.assignedStaff ms WHERE ms.id = :viewerId))
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findByFilters(
            @Param("status") Status status,
            @Param("priority") Priority priority,
            @Param("issueType") IssueType issueType,
            @Param("assignedToId") Long assignedToId,
            @Param("createdById") Long createdById,
            @Param("search") String search,
            @Param("viewerId") Long viewerId
    );
}