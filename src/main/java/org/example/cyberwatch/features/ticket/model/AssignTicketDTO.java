package org.example.cyberwatch.features.ticket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class AssignTicketDTO {

    private List<Long> staffIds;
}