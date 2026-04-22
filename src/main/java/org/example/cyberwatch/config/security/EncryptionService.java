package org.example.cyberwatch.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private final TextEncryptor textEncryptor;

    public String encrypt(String data) {
        return textEncryptor.encrypt(data);
    }

    public String decrypt(String data) {
        return textEncryptor.decrypt(data);
    }

    public String maskLastFour(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return "****";
        }
        String decrypted = textEncryptor.decrypt(encrypted);
        if (decrypted.length() <= 4) {
            return "****";
        }
        return decrypted.substring(0, decrypted.length() - 4) + "****";
    }
}
