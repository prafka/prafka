package com.prafka.desktop.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.LogModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogServiceTest {

    private ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
    private SettingsService settingsService = mock(SettingsService.class);
    private LogService logService = new LogService(applicationProperties, settingsService);

    @BeforeEach
    void setUp() {
        when(applicationProperties.userLogDir()).thenReturn("/tmp/logs");
        when(applicationProperties.isLogConsoleEnabled()).thenReturn("false");
        when(settingsService.getLog()).thenReturn(new LogModel());
    }

    @Test
    void shouldGetDir() {
        // When
        var result = logService.getDir();

        // Then
        assertEquals("/tmp/logs", result);
    }

    @Test
    void shouldSetDebugLevel() {
        // When
        logService.setDebug();

        // Then
        var rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.DEBUG, rootLogger.getLevel());
    }

    @Test
    void shouldSetInfoLevel() {
        // Given
        logService.setDebug();

        // When
        logService.setInfo();

        // Then
        var rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.INFO, rootLogger.getLevel());
    }

    @Test
    void shouldGetLogDirFromApplicationProperties() {
        // Given
        when(applicationProperties.userLogDir()).thenReturn("/custom/log/path");

        // When
        var result = logService.getDir();

        // Then
        assertEquals("/custom/log/path", result);
    }

    @Test
    void shouldToggleBetweenDebugAndInfo() {
        // Given
        logService.setInfo();

        // When
        logService.setDebug();
        var debugLevel = ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel();
        logService.setInfo();
        var infoLevel = ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel();

        // Then
        assertEquals(Level.DEBUG, debugLevel);
        assertEquals(Level.INFO, infoLevel);
    }
}
