ALTER TABLE ticket
    ADD COLUMN assignee_id BIGINT;

ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_assignee
        FOREIGN KEY (assignee_id) REFERENCES staff(id);