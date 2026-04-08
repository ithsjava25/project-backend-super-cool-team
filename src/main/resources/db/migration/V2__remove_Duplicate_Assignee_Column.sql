-- Closes issue #29: consolidate duplicate assignee fields in tickets table.
-- All assignment logic now uses assigned_to_id.

ALTER TABLE tickets DROP CONSTRAINT IF EXISTS FK_TICKETS_ON_ASSIGNEE;
ALTER TABLE tickets DROP COLUMN IF EXISTS assignee_id;