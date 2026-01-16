package com.prafka.desktop.service;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class I18nServiceTest {

    @Test
    void shouldLoadBundle() {
        // Given
        var settingsService = mock(SettingsService.class);
        when(settingsService.getLocale()).thenReturn(Locale.ENGLISH);
        var i18nService = new I18nService(settingsService);

        // When
        var result = i18nService.get("common.ok");

        // Then
        assertEquals("OK", result);
    }
}