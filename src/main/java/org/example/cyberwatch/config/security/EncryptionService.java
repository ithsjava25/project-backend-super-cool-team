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

    public String encrypt(String data) {
        return textEncryptor.encrypt(data);
    }

    public String decrypt(String data) {
        return textEncryptor.decrypt(data);
    }

    /**
     * Skapar ett Blind Index (deterministisk hash).
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
