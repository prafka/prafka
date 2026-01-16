package com.prafka.desktop.service;

import com.prafka.desktop.model.PlainStorageModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MasterPasswordServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private CryptoService cryptoService = mock(CryptoService.class);
    private MasterPasswordService masterPasswordService = new MasterPasswordService(storageService, cryptoService);

    @Test
    void shouldSaveMasterPassword() {
        // Given
        var plainStorage = spy(PlainStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);
        when(cryptoService.hashPasswordForStorage(anyString())).thenReturn("hash");
        when(cryptoService.encodeBase64(anyString())).thenReturn("base64");

        // When
        masterPasswordService.saveMasterPassword("password");

        // Then
        assertEquals("base64", plainStorage.getMasterPassword());
        verify(storageService).savePlainStorage();
    }
}