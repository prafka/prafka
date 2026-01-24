package com.prafka.desktop.service;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.EncryptedStorageModel;
import com.prafka.desktop.model.PlainStorageModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import static com.prafka.core.util.JsonFactory.gsonDefault;

/**
 * Manages persistent storage for application data using encrypted and plain storage files.
 *
 * <p>Handles loading and saving of both plain-text settings and encrypted data
 * (such as cluster credentials) using a master password-protected keystore.
 */
@Singleton
public class StorageService {

    private final SessionService sessionService;
    private final CryptoService cryptoService;
    private final String plainStorageFileName;
    private final String encryptedStorageFileName;
    private final String keyStoreFileName;
    private final String encryptedStorageKeyStoreAlias;
    private PlainStorageModel plainStorage = new PlainStorageModel();
    private EncryptedStorageModel encryptedStorage = new EncryptedStorageModel();

    @Inject
    public StorageService(ApplicationProperties applicationProperties, SessionService sessionService, CryptoService cryptoService) {
        this.sessionService = sessionService;
        this.cryptoService = cryptoService;
        plainStorageFileName = applicationProperties.userDataDir() + "/plain-storage.db";
        encryptedStorageFileName = applicationProperties.userDataDir() + "/encrypted-storage.db";
        keyStoreFileName = applicationProperties.userDataDir() + "/keystore.p12";
        encryptedStorageKeyStoreAlias = "encrypted-storage-secret";
    }

    public PlainStorageModel getPlainStorage() {
        return plainStorage;
    }

    public EncryptedStorageModel getEncryptedStorage() {
        return encryptedStorage;
    }

    public void loadPlainStorage() {
        try {
            var fileBase64 = Files.readAllBytes(Path.of(plainStorageFileName));
            var file = cryptoService.decodeBase64(fileBase64);
            plainStorage = gsonDefault.fromJson(new String(file, StandardCharsets.UTF_8), PlainStorageModel.class);
            migratePlainStorage();
        } catch (NoSuchFileException e) {
            savePlainStorage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void migratePlainStorage() {
        if (ApplicationProperties.VERSION.equals(plainStorage.getVersion())) return;
        plainStorage.setVersion(ApplicationProperties.VERSION);
        savePlainStorage();
    }

    public void savePlainStorage() {
        try {
            var json = gsonDefault.toJson(plainStorage).getBytes(StandardCharsets.UTF_8);
            var jsonBase64 = cryptoService.encodeBase64(json);
            Files.write(Path.of(plainStorageFileName), jsonBase64, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void loadEncryptedStorage() {
        try {
            var encryptedFileBase64 = Files.readAllBytes(Path.of(encryptedStorageFileName));
            var encryptedFile = cryptoService.decodeBase64(encryptedFileBase64);
            var decryptedFile = cryptoService.decryptAES(encryptedFile, loadSecretKey());
            encryptedStorage = gsonDefault.fromJson(new String(decryptedFile, StandardCharsets.UTF_8), EncryptedStorageModel.class);
            migrateEncryptedStorage();
        } catch (NoSuchFileException e) {
            saveEncryptedStorage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void migrateEncryptedStorage() {
        if (ApplicationProperties.VERSION.equals(encryptedStorage.getVersion())) return;
        encryptedStorage.setVersion(ApplicationProperties.VERSION);
        saveEncryptedStorage();
    }

    public void saveEncryptedStorage() {
        try {
            var json = gsonDefault.toJson(encryptedStorage);
            var encryptedJson = cryptoService.encryptAES(json.getBytes(StandardCharsets.UTF_8), loadSecretKey());
            var encryptedJsonBase64 = cryptoService.encodeBase64(encryptedJson);
            Files.write(Path.of(encryptedStorageFileName), encryptedJsonBase64, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteEncryptedStorage() {
        try {
            Files.deleteIfExists(Path.of(keyStoreFileName));
            Files.deleteIfExists(Path.of(encryptedStorageFileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey loadSecretKey() throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, CertificateException, IOException {
        var keyStore = loadKeyStore();
        var passwordProtection = new KeyStore.PasswordProtection(sessionService.getMasterPassword());
        if (keyStore.containsAlias(encryptedStorageKeyStoreAlias)) {
            var secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(encryptedStorageKeyStoreAlias, passwordProtection);
            return secretKeyEntry.getSecretKey();
        } else {
            var keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            var secretKeyEntry = new KeyStore.SecretKeyEntry(keyGenerator.generateKey());
            keyStore.setEntry(encryptedStorageKeyStoreAlias, secretKeyEntry, passwordProtection);
            saveKeyStore(keyStore);
            return secretKeyEntry.getSecretKey();
        }
    }

    private KeyStore loadKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        var keyStore = KeyStore.getInstance("pkcs12");
        try (var fis = new FileInputStream(keyStoreFileName)) {
            keyStore.load(fis, sessionService.getMasterPassword());
        } catch (FileNotFoundException e) {
            keyStore.load(null, sessionService.getMasterPassword());
            saveKeyStore(keyStore);
        }
        return keyStore;
    }

    private void saveKeyStore(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        try (var fos = new FileOutputStream(keyStoreFileName)) {
            keyStore.store(fos, sessionService.getMasterPassword());
        }
    }
}
