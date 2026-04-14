ALTER TABLE tickets
    ADD COLUMN ticket_code VARCHAR(50);

UPDATE tickets
SET ticket_code = CONCAT('TICKET-', id + 1000)
WHERE ticket_code IS NULL;

ALTER TABLE tickets
    ALTER COLUMN ticket_code SET NOT NULL;

ALTER TABLE tickets
    ADD CONSTRAINT uc_tickets_ticket_code UNIQUE (ticket_code);