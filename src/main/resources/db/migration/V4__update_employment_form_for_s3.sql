-- Add S3-reference för CV i formuläret
ALTER TABLE employment_form
    ADD COLUMN employed_s3_key VARCHAR(500);
