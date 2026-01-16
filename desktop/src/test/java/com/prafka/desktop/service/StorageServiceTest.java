package com.prafka.desktop.service;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.model.ThemeModel;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageServiceTest {

    private ApplicationProperties applicationProperties = mock(ApplicationProperties.class);
    private SessionService sessionService = mock(SessionService.class);
    private StorageService storageService;
    private String userDataDir = "target/test-user-dir";

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(Path.of(userDataDir));
        when(applicationProperties.userDataDir()).thenReturn(userDataDir);
        when(sessionService.getMasterPassword()).thenReturn("123456".toCharArray());
        storageService = new StorageService(applicationProperties, sessionService, new CryptoService());
    }

    @AfterEach
    void teatDown() throws Exception {
        FileUtils.deleteDirectory(Path.of(userDataDir).toFile());
    }

    @Test
    void shouldSavePlainStorage() {
        // Given
        assertTrue(Files.notExists(Path.of(userDataDir, "plain-storage.db")));

        // When
        storageService.savePlainStorage();

        // Then
        assertTrue(Files.exists(Path.of(userDataDir, "plain-storage.db")));
    }

    @Test
    void shouldLoadPlainStorage() {
        // Given
        storageService.savePlainStorage();
        storageService.getPlainStorage().setTheme("wrong");

        // When
        storageService.loadPlainStorage();

        // Then
        assertEquals(ThemeModel.LIGHT.getCode(), storageService.getPlainStorage().getTheme());
    }

    @Test
    void shouldSaveEncryptedStorage() {
        // Given
        assertTrue(Files.notExists(Path.of(userDataDir, "encrypted-storage.db")));

        // When
        storageService.saveEncryptedStorage();

        // Then
        assertTrue(Files.exists(Path.of(userDataDir, "encrypted-storage.db")));
    }

    @Test
    void shouldLoadEncryptedStorage() {
        // Given
        storageService.getEncryptedStorage().setClusters(List.of(new ClusterModel(), new ClusterModel()));
        storageService.saveEncryptedStorage();
        storageService.getEncryptedStorage().setClusters(Collections.emptyList());

        // When
        storageService.loadEncryptedStorage();

        // Then
        assertEquals(2, storageService.getEncryptedStorage().getClusters().size());
    }
}