CREATE TABLE ticket_assignments
(
    ticket_id BIGINT NOT NULL,
    staff_id  BIGINT NOT NULL,
    CONSTRAINT pk_ticket_assignments PRIMARY KEY (ticket_id, staff_id),
    CONSTRAINT fk_ticket_assignments_on_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_assignments_on_staff FOREIGN KEY (staff_id) REFERENCES staff (employee_id) ON DELETE CASCADE
);

ALTER TABLE tickets DROP CONSTRAINT IF EXISTS FK_TICKETS_ON_ASSIGNED_TO;
ALTER TABLE tickets DROP COLUMN IF EXISTS assigned_to_id;
