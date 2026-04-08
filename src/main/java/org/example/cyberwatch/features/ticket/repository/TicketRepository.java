package org.example.cyberwatch.features.ticket.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.shared.model.enums.Status;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Ticket> findAll(); // hämtar nu allt i en enda JOIN-query

    List<Ticket> findByStatus(Status status);
    List<Ticket> findByAssignedToId(Long staffId);
    List<Ticket> findByCreatedById(Long staffId);
}