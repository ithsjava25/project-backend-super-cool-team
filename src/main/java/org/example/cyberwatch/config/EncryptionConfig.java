package org.example.cyberwatch.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

    @Value("${app.encryption.password}")
    private String encryptionPass;

    @Value("${app.encryption.pepper}")
    private String pepper;

    //Vi implementerade fail-fast validation för alla krypteringsnycklar. Applikationen startar INTE om någon nyckel är tom eller för svag.
    @PostConstruct
    public void validateEncryptionKeys() {
        if (encryptionPass == null || encryptionPass.isBlank()) {
            throw new IllegalArgumentException(
                    "CRITICAL: app.encryption.password is missing or blank. " +
                            "AES-256 encryption disabled. Set a strong password in environment variables."
            );
        }

        if (pepper == null || pepper.isBlank()) {
            throw new IllegalArgumentException(
                    "CRITICAL: app.encryption.pepper is missing or blank. " +
                            "Key derivation weakened. Set a strong pepper value in environment variables."
            );
        }

        // Minsta entropy-krav för password: 32 tecken
        if (encryptionPass.length() < 32) {
            throw new IllegalArgumentException(
                    "CRITICAL: app.encryption.password is too short (" + encryptionPass.length() + " chars). " +
                            "Requires minimum 32 characters for AES-256 security. " +
                            "Use a strong random password like: openssl rand -base64 24"
            );
        }

        // Minsta entropy-krav för pepper: 16 tecken
        if (pepper.length() < 16) {
            throw new IllegalArgumentException(
                    "CRITICAL: app.encryption.pepper is too short (" + pepper.length() + " chars). " +
                            "Requires minimum 16 characters. Use: openssl rand -hex 8"
            );
        }
    }

    /**
     * pass + pepper = key, delux kör dem genom en nyckelderivationsfunktion (PBKDF2)
     * för att skapa den faktiska AES-nyckeln som vi sedan använder för att kunna låsa upp/låsa personnummer
     * delux lägger till ett randomiserat salt-värde(Nonce/IV) vid varje anrop av encrypt
     * delux-objektet lever i minnet under hela applikationens körning - Spring hanterar livscykeln som en singleton Bean,
     * så det randomiserade saltet är unikt per kryptering och gör att samma input ger olika output varje gång
     * Det håller internt koll på:
     * encryptionPass + pepper → för att kunna kryptera/dekryptera och sätter in/plockar ut IV(saltet) vid encrypt/decrypt
     * Logiken för att generera ett nytt slumpmässigt IV/salt vid varje encrypt()-anrop
     *
     * Säkerhet: Om password eller pepper är tom/svag kastas IllegalArgumentException vid startup (@PostConstruct)
     */
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.delux(encryptionPass, pepper);
    }
}
