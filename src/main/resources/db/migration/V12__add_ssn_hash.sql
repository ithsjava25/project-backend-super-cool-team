ALTER TABLE staff
    ADD COLUMN ssn_hash VARCHAR(64);
ADD COLUMN ssn_salt VARCHAR(32);

ALTER TABLE employment_forms
    ADD COLUMN ssn_hash VARCHAR(64);
ADD COLUMN ssn_salt VARCHAR(32);

CREATE UNIQUE INDEX idx_staff_ssn_hash ON staff (ssn_hash);
CREATE UNIQUE INDEX idx_form_ssn_hash ON employment_form (ssn_hash);

--Uniqueness sköts via ssn_hash istället
ALTER TABLE staff DROP CONSTRAINT IF EXISTS staff_social_security_number_key;
ALTER TABLE employment_form DROP CONSTRAINT IF EXISTS employment_form_social_security_number_key;