-- Set default value for status and update existing NULL values
UPDATE staff SET status = 'OFFLINE' WHERE status IS NULL;

ALTER TABLE staff ALTER COLUMN status SET DEFAULT 'OFFLINE';
ALTER TABLE staff ALTER COLUMN status SET NOT NULL;
