package org.example.cyberwatch.config;

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

    /**
     * pass + pepper = key, delux kör dem genom en nyckelderivationsfunktion (PBKDF2)
     * för att skapa den faktiska AES-nyckeln som vi sedan använder för att kunna låsa upp/låsa personnummer
     * delux lägger till ett randomiserat salt-värde(Nonce/IV) vid varje anrop av encrypt
     * delux-objektet lever i minnet under hela applikationens körning - Spring hanterar livscykeln som en singleton Bean,
     * så det randomiserade saltet är unikt per kryptering och gör att samma input ger olika output varje gång
     * Det håller internt koll på:
     * encryptionPass + pepper → för att kunna kryptera/dekryptera och sätter in/plockar ut IV(saltet) vid encrypt/decrypt
     * Logiken för att generera ett nytt slumpmässigt IV/salt vid varje encrypt()-anrop
     */
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.delux(encryptionPass, pepper);
    }
}
