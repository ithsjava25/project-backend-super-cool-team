package org.example.cyberwatch.features.ticket.repository;

import org.example.cyberwatch.features.ticket.model.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
}