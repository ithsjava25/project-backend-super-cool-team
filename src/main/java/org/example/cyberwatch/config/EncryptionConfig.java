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

    //Skapar en AES-256 krypteringsnyckel, pass + pepper = key,
    //delux lägger till ett randomiserat salt-värde(Nonce/IV) vid varje anrop av encrypt
    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.delux(encryptionPass, pepper);
    }
}
