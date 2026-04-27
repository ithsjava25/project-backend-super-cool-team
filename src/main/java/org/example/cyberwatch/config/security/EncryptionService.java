package org.example.cyberwatch.config.security;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class EncryptionService {

    private final TextEncryptor textEncryptor;

    @Value("${app.encryption.hmac-key}") // En separat fast nyckel för sökning
    private String hmacKey;

    /**
     * Delegerar till delux som genererar ett unikt IV, krypterar med AES-256 nycken
     * och returnerar IV + krypterad data som en enda sträng, det är denna som sedan lagras i databasen.
     */
    public String encrypt(String data) {
        return textEncryptor.encrypt(data);
    }

    /**
     * Delegerar till delux som plockar ut IV från strängen och dekrypterar
     * med AES-256 för att återge klartexten.
     */
    public String decrypt(String data) {
        return textEncryptor.decrypt(data);
    }

    /**
     * Skapar ett "Blind Index" (deterministisk hash, sökbar).
     * Används för att jämföra om personnummer redan finns i databasen.
     */
    public String hmac(String data) {
        if (data == null) return null;
        return new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hmacKey).hmacHex(data);
    }

    public String maskLastFour(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return "****";
        }
        if (plainText.length() <= 4) {
            return "****";
        }
        return plainText.substring(0, plainText.length() - 4) + "****";
    }
}
