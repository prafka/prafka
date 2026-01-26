package com.prafka.desktop.service;

import com.prafka.desktop.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private PlainStorageModel plainStorage;
    private EncryptedStorageModel encryptedStorage;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        plainStorage = spy(PlainStorageModel.class);
        encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        settingsService = new SettingsService(storageService);
    }

    @Test
    void shouldGetLocale() {
        // Given
        plainStorage.setLocale("en");

        // When
        var result = settingsService.getLocale();

        // Then
        assertEquals(Locale.ENGLISH, result);
    }

    @Test
    void shouldSaveLocale() {
        // Given
        plainStorage.setLocale("en");

        // When
        settingsService.saveLocale(Locale.FRENCH);

        // Then
        assertEquals("fr", plainStorage.getLocale());
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldNotSaveLocaleWhenUnchanged() {
        // Given
        plainStorage.setLocale("en");

        // When
        settingsService.saveLocale(Locale.ENGLISH);

        // Then
        verify(storageService, never()).savePlainStorage();
    }

    @Test
    void shouldGetTheme() {
        // Given
        plainStorage.setTheme("light");

        // When
        var result = settingsService.getTheme();

        // Then
        assertEquals(ThemeModel.LIGHT, result);
    }

    @Test
    void shouldSaveTheme() {
        // Given
        plainStorage.setTheme("light");

        // When
        settingsService.saveTheme(ThemeModel.DARK);

        // Then
        assertEquals("dark", plainStorage.getTheme());
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldNotSaveThemeWhenUnchanged() {
        // Given
        plainStorage.setTheme("dark");

        // When
        settingsService.saveTheme(ThemeModel.DARK);

        // Then
        verify(storageService, never()).savePlainStorage();
    }

    @Test
    void shouldGetTimestampFormat() {
        // Given
        plainStorage.setTimestampFormat("yyyymmdd");

        // When
        var result = settingsService.getTimestampFormat();

        // Then
        assertEquals(TimestampFormatModel.YYYY_MM_DD, result);
    }

    @Test
    void shouldSaveTimestampFormat() {
        // Given
        plainStorage.setTimestampFormat("yyyymmdd");

        // When
        settingsService.saveTimestampFormat(TimestampFormatModel.DD_MM_YYYY);

        // Then
        assertEquals("ddmmyyyy", plainStorage.getTimestampFormat());
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldNotSaveTimestampFormatWhenUnchanged() {
        // Given
        plainStorage.setTimestampFormat("ddmmyyyy");

        // When
        settingsService.saveTimestampFormat(TimestampFormatModel.DD_MM_YYYY);

        // Then
        verify(storageService, never()).savePlainStorage();
    }

    @Test
    void shouldGetProxy() {
        // Given
        var proxy = new ProxyModel();
        proxy.setHost("test-host");
        encryptedStorage.setProxy(proxy);

        // When
        var result = settingsService.getProxy();

        // Then
        assertEquals("test-host", result.getHost());
    }

    @Test
    void shouldSaveProxy() {
        // Given
        var proxy = new ProxyModel();
        proxy.setHost("new-host");

        // When
        settingsService.saveProxy(proxy);

        // Then
        verify(encryptedStorage).setProxy(proxy);
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldGetLog() {
        // Given
        var log = new LogModel();
        log.setDebug(true);
        plainStorage.setLog(log);

        // When
        var result = settingsService.getLog();

        // Then
        assertTrue(result.isDebug());
    }

    @Test
    void shouldSaveLog() {
        // Given
        var log = new LogModel();
        log.setDebug(true);

        // When
        settingsService.saveLog(log);

        // Then
        verify(plainStorage).setLog(log);
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldGetStageWidthWithDefault() {
        // When
        var result = settingsService.getStageWidth("test-stage", 800);

        // Then
        assertEquals(800, result);
    }

    @Test
    void shouldSaveStageWidth() {
        // When
        settingsService.saveStageWidth("test-stage", 1024);

        // Then
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldGetStageHeightWithDefault() {
        // When
        var result = settingsService.getStageHeight("test-stage", 600);

        // Then
        assertEquals(600, result);
    }

    @Test
    void shouldSaveStageHeight() {
        // When
        settingsService.saveStageHeight("test-stage", 768);

        // Then
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldGetAvailableLocales() {
        // When
        var result = settingsService.getAvailableLocales();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldGetAvailableThemes() {
        // When
        var result = settingsService.getAvailableThemes();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void shouldGetAvailableTimestampFormats() {
        // When
        var result = settingsService.getAvailableTimestampFormats();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
