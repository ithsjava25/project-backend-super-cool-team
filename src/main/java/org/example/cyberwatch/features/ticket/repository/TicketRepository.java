package org.example.cyberwatch.features.ticket.repository;

import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Hitta alla tickets med en viss status, t.ex. alla OPEN
    List<Ticket> findByStatus(Status status);

    // Hitta alla tickets tilldelade till en specifik handläggare
    List<Ticket> findByAssignedToId(Long staffId);

    // Hitta alla tickets skapade av en specifik användare
    List<Ticket> findByCreatedById(Long staffId);
}