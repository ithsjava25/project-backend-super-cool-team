INSERT INTO staff (social_security_number, first_name, last_name, email, phone_number, role, department, password)
VALUES ('19980508-3872', 'Caroline', 'Nordbrandt', 'nordbrandtcaroline@gmail.com', '0936458263', 'ADMIN', 'MANAGEMENT',
        '$2a$12$DfSHWL9xvGe/WSRNVpAww.FCn0oJ4TmNyIXbLgtqIsl.k3D9MraQ2')
ON CONFLICT (social_security_number) DO NOTHING;