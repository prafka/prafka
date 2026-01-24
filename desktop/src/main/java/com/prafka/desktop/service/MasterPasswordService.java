package com.prafka.desktop.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Manages the master password used to protect encrypted application data.
 *
 * <p>Provides methods to save and verify the master password using secure
 * hashing algorithms.
 */
@Singleton
public class MasterPasswordService {

    private final StorageService storageService;
    private final CryptoService cryptoService;

    @Inject
    public MasterPasswordService(StorageService storageService, CryptoService cryptoService) {
        this.storageService = storageService;
        this.cryptoService = cryptoService;
    }

    public void saveMasterPassword(String masterPassword) {
        var hash = cryptoService.hashPasswordForStorage(masterPassword);
        var base64 = cryptoService.encodeBase64(hash);
        storageService.getPlainStorage().setMasterPassword(base64);
        storageService.savePlainStorage();
    }

    public boolean checkMasterPassword(String masterPassword) {
        var hash = cryptoService.decodeBase64(storageService.getPlainStorage().getMasterPassword());
        return cryptoService.checkPasswordForStorage(masterPassword, hash);
    }
}
