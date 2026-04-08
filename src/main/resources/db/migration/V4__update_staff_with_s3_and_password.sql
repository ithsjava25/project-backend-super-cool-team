-- Update Staff to handle password and S3
ALTER TABLE staff
    ADD COLUMN password VARCHAR(255), -- OAuth2 password grant/form login
ADD COLUMN employed_s3_key VARCHAR(500),
-- S3 reference for attachments related to staff

-- Make email in staff unique to prevent duplicate entries and ensure proper identification of staff members
ALTER TABLE staff
    ADD CONSTRAINT uc_staff_email UNIQUE (email);