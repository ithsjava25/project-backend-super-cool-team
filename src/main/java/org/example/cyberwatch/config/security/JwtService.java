package org.example.cyberwatch.config.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Hanterar generering och validering av JWT-tokens.
 *
 * Efter att Google autentiserat en användare genererar vi en egen JWT-token
 * som frontend sedan skickar med varje API-anrop i Authorization-headern.
 * Det gör att vi slipper prata med Google på varje request.
 */
@Service
public class JwtService {

    // Hemlig nyckel för att signera tokens — hämtas från application.properties / .env
    @Value("${app.jwt.secret}")
    private String secret;

    // Tokens är giltiga i 24 timmar
    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24;

    // Bygger en SecretKey av den hemliga strängen för HMAC-SHA256-signering
    private SecretKey getKey() {
        byte[] keyBytes = Base64.getEncoder().encode(secret.getBytes());
        return new SecretKeySpec(keyBytes, 0, 32, "HmacSHA256");
    }

    /**
     * Genererar en JWT-token med email och roll som claims.
     * Anropas från OAuth2SuccessHandler när en känd användare loggar in via Google.
     */
    public String generateToken(String email, String role) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(email)                         // email används som identifierare
                    .claim("role", role)                    // rollen inkluderas så frontend vet vad användaren får göra
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusMillis(EXPIRATION_MS)))
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(getKey().getEncoded()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Kunde inte generera JWT-token", e);
        }
    }

    /**
     * Extraherar emailen (subject) ur en token.
     * Används av JwtAuthFilter för att slå upp rätt Staff i databasen.
     */
    public String extractEmail(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validerar att tokenen är korrekt signerad och inte har gått ut.
     * Om något är fel returneras false och requesten nekas i JwtAuthFilter.
     */
    public boolean isTokenValid(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            boolean validSignature = jwt.verify(new MACVerifier(getKey().getEncoded()));
            boolean notExpired = jwt.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now()));
            return validSignature && notExpired;
        } catch (Exception e) {
            return false;
        }
    }
}