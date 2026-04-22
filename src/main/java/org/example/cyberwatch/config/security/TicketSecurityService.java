package org.example.cyberwatch.config.security;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hanterar ticket-specifik behörighetskontroll för @PreAuthorize-uttryck.
 *
 * Rollhierarki:
 *
 * Förhöjda roller (ADMIN, CEO, CTO):
 *   – Ser och kan ändra ALLA ärenden oavsett tilldelning
 *
 * Standardroller (HR, PROJECT_MANAGER, CONSULTANT):
 *   – Ser och kan ändra BARA ärenden de skapat eller är tilldelade till
 *   – Nekade åtkomstförsök loggas med staffId och roll
 */
@Component("ticketSecurity")
public class TicketSecurityService {

    private static final Logger log = LoggerFactory.getLogger(TicketSecurityService.class);

    private final TicketRepository ticketRepository;

    public TicketSecurityService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Kontrollerar om den inloggade användaren får se eller ändra ett specifikt ärende.
     * Nekade försök loggas med staffId, roll och ticketId.
     */
    @Transactional(readOnly = true)
    public boolean canAccess(Authentication authentication, Long ticketId) {
        if (!(authentication.getPrincipal() instanceof Staff requester)) return false;
        if (isElevatedRole(requester.getRole())) return true;

        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    boolean access = hasAccess(ticket, requester);
                    if (!access) {
                        log.warn("Åtkomst nekad för staffId={} role={} — försökte nå ticketId={}",
                                requester.getId(), requester.getRole(), ticketId);
                    }
                    return access;
                })
                .orElse(false);
    }

    /**
     * Samma behörighetskontroll som canAccess men för endpoints som
     * identifierar ärendet via ticketCode istället för ID.
     */
    @Transactional(readOnly = true)
    public boolean canAccessByCode(Authentication authentication, String ticketCode) {
        if (!(authentication.getPrincipal() instanceof Staff requester)) return false;
        if (isElevatedRole(requester.getRole())) return true;

        return ticketRepository.findByTicketCode(ticketCode)
                .map(ticket -> {
                    boolean access = hasAccess(ticket, requester);
                    if (!access) {
                        log.warn("Åtkomst nekad för staffId={} role={} — försökte nå ticketCode={}",
                                requester.getId(), requester.getRole(), ticketCode);
                    }
                    return access;
                })
                .orElse(false);
    }

    private boolean hasAccess(Ticket ticket, Staff requester) {
        boolean isOwner = ticket.getCreatedBy() != null &&
                ticket.getCreatedBy().getId().equals(requester.getId());
        boolean isAssigned = ticket.getAssignedStaff().stream()
                .anyMatch(s -> s.getId().equals(requester.getId()));
        return isOwner || isAssigned;
    }

    private boolean isElevatedRole(Role role) {
        return role == Role.ADMIN || role == Role.CEO || role == Role.CTO;
    }
}