package com.prafka.desktop;

import javafx.application.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationPropertiesTest {

    @TempDir
    Path tempDir;

    private Application mockApplication;
    private Application.Parameters mockParameters;

    @BeforeEach
    void setUp() {
        mockApplication = mock(Application.class);
        mockParameters = mock(Application.Parameters.class);
        when(mockApplication.getParameters()).thenReturn(mockParameters);
    }

    @Test
    void shouldLoadNameFromBuildProperties() {
        assertNotNull(ApplicationProperties.NAME);
        assertFalse(ApplicationProperties.NAME.isBlank());
    }

    @Test
    void shouldLoadVersionFromBuildProperties() {
        assertNotNull(ApplicationProperties.VERSION);
        assertFalse(ApplicationProperties.VERSION.isBlank());
    }

    @Test
    void shouldCreateUserDataDirectory() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        new ApplicationProperties(mockApplication);

        // Then
        assertTrue(Files.exists(userDataDir));
        assertTrue(Files.isDirectory(userDataDir));
    }

    @Test
    void shouldCreateUserLogDirectory() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        new ApplicationProperties(mockApplication);

        // Then
        assertTrue(Files.exists(userLogDir));
        assertTrue(Files.isDirectory(userLogDir));
    }

    @Test
    void shouldUseCustomUserDataDirFromParameters() {
        // Given
        var customDataDir = tempDir.resolve("custom-data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", customDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertEquals(customDataDir.toString(), properties.userDataDir());
    }

    @Test
    void shouldUseCustomUserLogDirFromParameters() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var customLogDir = tempDir.resolve("custom-logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", customLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertEquals(customLogDir.toString(), properties.userLogDir());
    }

    @Test
    void shouldReturnUserDataDir() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertEquals(userDataDir.toString(), properties.userDataDir());
    }

    @Test
    void shouldReturnUserLogDir() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertEquals(userLogDir.toString(), properties.userLogDir());
    }

    @Test
    void shouldReturnLogConsoleEnabledDefault() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertEquals("false", properties.isLogConsoleEnabled());
    }

    @Test
    void shouldReturnTrackStageSizeDefault() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertTrue(properties.isTrackStageSize());
    }

    @Test
    void shouldReturnDomain() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertNotNull(properties.domain());
        assertFalse(properties.domain().isBlank());
    }

    @Test
    void shouldReturnApiUrl() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertNotNull(properties.apiUrl());
        assertTrue(properties.apiUrl().contains(properties.domain()));
    }

    @Test
    void shouldReturnDocsUrl() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertNotNull(properties.docsUrl());
        assertTrue(properties.docsUrl().contains(properties.domain()));
    }

    @Test
    void shouldReturnDistUrl() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertNotNull(properties.distUrl());
        assertTrue(properties.distUrl().contains(properties.domain()));
    }

    @Test
    void shouldReturnEmail() {
        // Given
        var userDataDir = tempDir.resolve("data");
        var userLogDir = tempDir.resolve("logs");
        when(mockParameters.getNamed()).thenReturn(Map.of(
            "userDataDir", userDataDir.toString(),
            "userLogDir", userLogDir.toString()
        ));

        // When
        var properties = new ApplicationProperties(mockApplication);

        // Then
        assertNotNull(properties.email());
        assertTrue(properties.email().contains(properties.domain()));
    }
}
