ALTER TABLE staff
    ADD COLUMN ssn_hash VARCHAR(64);
ALTER TABLE employment_forms
    ADD COLUMN ssn_hash VARCHAR(64);