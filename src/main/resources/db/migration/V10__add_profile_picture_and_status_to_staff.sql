-- Add profile_picture_url and status to staff table
ALTER TABLE staff
    ADD COLUMN profile_picture_url VARCHAR(500),
    ADD COLUMN status VARCHAR(50);
