package com.prafka.desktop.service;

import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    @Test
    void shouldEncryptDecryptAES() throws Exception {
        // Given
        var cryptoService = new CryptoService();

        var keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, "master-password".toCharArray());

        var passwordProtection = new KeyStore.PasswordProtection("master-password".toCharArray());

        var keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);

        var secretKeyEntry = new KeyStore.SecretKeyEntry(keyGenerator.generateKey());
        keyStore.setEntry("encrypted-storage-secret", secretKeyEntry, passwordProtection);

        var secretKey = secretKeyEntry.getSecretKey();

        // When
        var encrypted = cryptoService.encryptAES("test-data".getBytes(StandardCharsets.UTF_8), secretKey);
        var decrypted = cryptoService.decryptAES(encrypted, secretKey);

        // Then
        assertEquals("test-data", new String(decrypted));
    }
}