package com.prafka.desktop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private CryptoService cryptoService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() throws Exception {
        cryptoService = new CryptoService();

        var keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, "master-password".toCharArray());

        var passwordProtection = new KeyStore.PasswordProtection("master-password".toCharArray());

        var keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);

        var secretKeyEntry = new KeyStore.SecretKeyEntry(keyGenerator.generateKey());
        keyStore.setEntry("encrypted-storage-secret", secretKeyEntry, passwordProtection);

        secretKey = secretKeyEntry.getSecretKey();
    }

    @Test
    void shouldEncryptDecryptAES() throws Exception {
        // When
        var encrypted = cryptoService.encryptAES("test-data".getBytes(StandardCharsets.UTF_8), secretKey);
        var decrypted = cryptoService.decryptAES(encrypted, secretKey);

        // Then
        assertEquals("test-data", new String(decrypted));
    }

    @Test
    void shouldHashPasswordForStorage() {
        // Given
        var password = "test-password";

        // When
        var hash = cryptoService.hashPasswordForStorage(password);

        // Then
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void shouldCheckPasswordForStorageReturnTrue() {
        // Given
        var password = "test-password";
        var hash = cryptoService.hashPasswordForStorage(password);

        // When
        var result = cryptoService.checkPasswordForStorage(password, hash);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCheckPasswordForStorageReturnFalse() {
        // Given
        var password = "test-password";
        var hash = cryptoService.hashPasswordForStorage(password);

        // When
        var result = cryptoService.checkPasswordForStorage("wrong-password", hash);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldHashPasswordForSession() {
        // Given
        var password = "test-password";

        // When
        var hash = cryptoService.hashPasswordForSession(password);

        // Then
        assertNotNull(hash);
        assertEquals(128, hash.length()); // SHA-512 produces 64 bytes = 128 hex chars
    }

    @Test
    void shouldHashPasswordForSessionConsistent() {
        // Given
        var password = "test-password";

        // When
        var hash1 = cryptoService.hashPasswordForSession(password);
        var hash2 = cryptoService.hashPasswordForSession(password);

        // Then
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldSha512ReturnConsistentHash() {
        // Given
        var data = "test-data";

        // When
        var hash1 = cryptoService.sha512(data);
        var hash2 = cryptoService.sha512(data);

        // Then
        assertArrayEquals(hash1, hash2);
    }

    @Test
    void shouldSha512Return64Bytes() {
        // Given
        var data = "test-data";

        // When
        var hash = cryptoService.sha512(data);

        // Then
        assertEquals(64, hash.length);
    }

    @Test
    void shouldEncodeDecodeBase64String() {
        // Given
        var data = "test-string";

        // When
        var encoded = cryptoService.encodeBase64(data);
        var decoded = cryptoService.decodeBase64(encoded);

        // Then
        assertEquals(data, decoded);
    }

    @Test
    void shouldEncodeDecodeBase64Bytes() {
        // Given
        var data = "test-bytes".getBytes(StandardCharsets.UTF_8);

        // When
        var encoded = cryptoService.encodeBase64(data);
        var decoded = cryptoService.decodeBase64(encoded);

        // Then
        assertArrayEquals(data, decoded);
    }

    @Test
    void shouldEncodeBase64EmptyString() {
        // Given
        var data = "";

        // When
        var encoded = cryptoService.encodeBase64(data);
        var decoded = cryptoService.decodeBase64(encoded);

        // Then
        assertEquals(data, decoded);
    }

    @Test
    void shouldEncryptAESProduceDifferentCiphertext() throws Exception {
        // Given
        var data = "test-data".getBytes(StandardCharsets.UTF_8);

        // When
        var encrypted1 = cryptoService.encryptAES(data, secretKey);
        var encrypted2 = cryptoService.encryptAES(data, secretKey);

        // Then - different IV should produce different ciphertext
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2));
    }

    @Test
    void shouldDecryptAESWithDifferentIV() throws Exception {
        // Given
        var data = "test-data".getBytes(StandardCharsets.UTF_8);

        // When
        var encrypted1 = cryptoService.encryptAES(data, secretKey);
        var encrypted2 = cryptoService.encryptAES(data, secretKey);
        var decrypted1 = cryptoService.decryptAES(encrypted1, secretKey);
        var decrypted2 = cryptoService.decryptAES(encrypted2, secretKey);

        // Then - both should decrypt to same plaintext
        assertArrayEquals(data, decrypted1);
        assertArrayEquals(data, decrypted2);
    }
}