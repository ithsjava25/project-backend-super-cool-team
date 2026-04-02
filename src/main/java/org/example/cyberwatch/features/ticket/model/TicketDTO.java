package org.example.cyberwatch.features.ticket.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;

@Getter
@Setter
public class TicketDTO {

    @NotBlank(message = "Titel får inte vara tom")
    @Size(min = 5, max = 100, message = "Titel måste vara mellan 5-100 tecken")
    private String title;

    @NotBlank(message = "Beskrivning får inte vara tom")
    @Size(max = 2000, message = "Beskrivning kan max vara 2000 tecken")
    private String description;

    @NotNull(message = "Prioritet måste anges")
    private Priority priority;

    @NotNull(message = "Ärendetyp måste anges")
    private IssueType issueType;

    @NotNull(message = "createdById måste anges")
    @Positive(message = "createdById måste vara ett positivt tal")
    private Long createdById;
}