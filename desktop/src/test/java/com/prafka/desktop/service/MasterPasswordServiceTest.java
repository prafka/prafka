package com.prafka.desktop.service;

import com.prafka.desktop.model.PlainStorageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MasterPasswordServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private CryptoService cryptoService = mock(CryptoService.class);
    private MasterPasswordService masterPasswordService;
    private PlainStorageModel plainStorage;

    @BeforeEach
    void setUp() {
        plainStorage = spy(PlainStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);
        masterPasswordService = new MasterPasswordService(storageService, cryptoService);
    }

    @Test
    void shouldSaveMasterPassword() {
        // Given
        when(cryptoService.hashPasswordForStorage(anyString())).thenReturn("hash");
        when(cryptoService.encodeBase64(anyString())).thenReturn("base64");

        // When
        masterPasswordService.saveMasterPassword("password");

        // Then
        assertEquals("base64", plainStorage.getMasterPassword());
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldCheckMasterPasswordReturnTrueForCorrect() {
        // Given
        plainStorage.setMasterPassword("encodedHash");
        when(cryptoService.decodeBase64("encodedHash")).thenReturn("storedHash");
        when(cryptoService.checkPasswordForStorage("correct-password", "storedHash")).thenReturn(true);

        // When
        var result = masterPasswordService.checkMasterPassword("correct-password");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCheckMasterPasswordReturnFalseForIncorrect() {
        // Given
        plainStorage.setMasterPassword("encodedHash");
        when(cryptoService.decodeBase64("encodedHash")).thenReturn("storedHash");
        when(cryptoService.checkPasswordForStorage("wrong-password", "storedHash")).thenReturn(false);

        // When
        var result = masterPasswordService.checkMasterPassword("wrong-password");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldSaveMasterPasswordHashFirst() {
        // Given
        when(cryptoService.hashPasswordForStorage("password")).thenReturn("hashed");
        when(cryptoService.encodeBase64("hashed")).thenReturn("base64hashed");

        // When
        masterPasswordService.saveMasterPassword("password");

        // Then
        var inOrder = inOrder(cryptoService);
        inOrder.verify(cryptoService).hashPasswordForStorage("password");
        inOrder.verify(cryptoService).encodeBase64("hashed");
    }

    @Test
    void shouldCheckMasterPasswordDecodeFirst() {
        // Given
        plainStorage.setMasterPassword("encoded");
        when(cryptoService.decodeBase64("encoded")).thenReturn("decoded");
        when(cryptoService.checkPasswordForStorage(anyString(), anyString())).thenReturn(true);

        // When
        masterPasswordService.checkMasterPassword("test");

        // Then
        verify(cryptoService).decodeBase64("encoded");
        verify(cryptoService).checkPasswordForStorage("test", "decoded");
    }
}