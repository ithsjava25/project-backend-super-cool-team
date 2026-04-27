ALTER TABLE staff
    ADD COLUMN ssn_hash VARCHAR(64);

ALTER TABLE employment_form
    ADD COLUMN ssn_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_staff_ssn_hash ON staff (ssn_hash);
CREATE UNIQUE INDEX idx_form_ssn_hash ON employment_form (ssn_hash);

--Uniqueness sköts via ssn_hash istället
ALTER TABLE staff DROP CONSTRAINT IF EXISTS uc_staff_social_security_number;
ALTER TABLE employment_form DROP CONSTRAINT IF EXISTS uc_employmentform_social_security_number;
