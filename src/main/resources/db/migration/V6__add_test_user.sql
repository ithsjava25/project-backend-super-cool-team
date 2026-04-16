INSERT INTO staff (social_security_number, first_name, last_name, email, phone_number, role, department, password)
VALUES ('19900101-1234', 'Eric', 'Thilen', 'ericthilen2003@gmail.com', '0701234567', 'ADMIN', 'MANAGEMENT', '$2a$10$w5pG/fM/yT.x/jZ0j2/H/.8yK5PzX3Y/O5F7y/6C6e9zX6y/O5F7y')
ON CONFLICT (social_security_number) DO NOTHING; -- Avoid duplicate key errors if already present
