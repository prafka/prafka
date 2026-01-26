package com.prafka.desktop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class I18nServiceTest {

    private SettingsService settingsService = mock(SettingsService.class);
    private I18nService i18nService;

    @BeforeEach
    void setUp() {
        when(settingsService.getLocale()).thenReturn(Locale.ENGLISH);
        i18nService = new I18nService(settingsService);
    }

    @Test
    void shouldLoadBundle() {
        // When
        var result = i18nService.get("common.ok");

        // Then
        assertEquals("OK", result);
    }

    @Test
    void shouldGetBundleReturnNonNull() {
        // When
        var bundle = i18nService.getBundle();

        // Then
        assertNotNull(bundle);
    }

    @Test
    void shouldGetThrowForMissingKey() {
        assertThrows(MissingResourceException.class, () -> i18nService.get("non.existent.key"));
    }

    @Test
    void shouldLoadBundleUpdateBundle() {
        // Given
        var initialBundle = i18nService.getBundle();

        // When
        i18nService.loadBundle();
        var reloadedBundle = i18nService.getBundle();

        // Then
        assertNotNull(reloadedBundle);
    }

    @Test
    void shouldGetReturnLocalizedString() {
        // When
        var result = i18nService.get("common.cancel");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void shouldBundleContainExpectedKeys() {
        // When
        var bundle = i18nService.getBundle();

        // Then
        assertTrue(bundle.containsKey("common.ok"));
        assertTrue(bundle.containsKey("common.cancel"));
    }
}