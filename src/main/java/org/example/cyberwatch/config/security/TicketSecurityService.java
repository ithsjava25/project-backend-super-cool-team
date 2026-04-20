package org.example.cyberwatch.config.security;

import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Hanterar ticket-specifik behörighetskontroll för @PreAuthorize-uttryck.
 *
 * Rollhierarki:
 *
 * Förhöjda roller (ADMIN, CEO, CTO):
 *   – Ser och kan ändra ALLA ärenden oavsett tilldelning
 *   – CEO och CTO behöver ledningsöversikt över alla pågående ärenden
 *   – ADMIN har dessutom exklusiv rätt att tilldela och radera ärenden
 *
 * Standardroller (HR, PROJECT_MANAGER, CONSULTANT):
 *   – Ser och kan ändra BARA ärenden de skapat eller är tilldelade till
 *   – Principen om minsta möjliga åtkomst för att skydda sekretess
 */
@Component("ticketSecurity")
public class TicketSecurityService {

    private final TicketRepository ticketRepository;

    public TicketSecurityService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Kontrollerar om den inloggade användaren får se eller ändra ett specifikt ärende.
     * Används av @PreAuthorize på endpoints som tar ticketId som path variable.
     *
     * @param authentication Spring Security-kontexten
     * @param ticketId       ID på det ärende som ska nås
     * @return true om åtkomst tillåts, false annars (→ 403)
     */
    public boolean canAccess(Authentication authentication, Long ticketId) {
        if (!(authentication.getPrincipal() instanceof Staff requester)) return false;

        // Förhöjda roller ser allt – ingen databasfråga behövs
        if (isElevatedRole(requester.getRole())) return true;

        // Standardroller – kontrollera om användaren är skapare eller tilldelad
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    boolean isOwner = ticket.getCreatedBy() != null &&
                            ticket.getCreatedBy().getId().equals(requester.getId());
                    boolean isAssigned = ticket.getAssignedStaff().stream()
                            .anyMatch(s -> s.getId().equals(requester.getId()));
                    return isOwner || isAssigned;
                })
                .orElse(false);
    }

    /**
     * Samma behörighetskontroll som canAccess men för endpoints som
     * identifierar ärendet via ticketCode istället för ID.
     *
     * @param authentication Spring Security-kontexten
     * @param ticketCode     ärendekoden, t.ex. "TICKET-1042"
     * @return true om åtkomst tillåts, false annars (→ 403)
     */
    public boolean canAccessByCode(Authentication authentication, String ticketCode) {
        if (!(authentication.getPrincipal() instanceof Staff requester)) return false;

        if (isElevatedRole(requester.getRole())) return true;

        return ticketRepository.findByTicketCode(ticketCode)
                .map(ticket -> {
                    boolean isOwner = ticket.getCreatedBy() != null &&
                            ticket.getCreatedBy().getId().equals(requester.getId());
                    boolean isAssigned = ticket.getAssignedStaff().stream()
                            .anyMatch(s -> s.getId().equals(requester.getId()));
                    return isOwner || isAssigned;
                })
                .orElse(false);
    }

    /**
     * Förhöjda roller har bredare åtkomst än standardroller.
     * ADMIN  – full systemkontroll
     * CEO    – ledningsöversikt och eskalering
     * CTO    – teknisk ledningsöversikt
     */
    private boolean isElevatedRole(Role role) {
        return role == Role.ADMIN || role == Role.CEO || role == Role.CTO;
    }
}