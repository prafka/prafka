package com.prafka.desktop.service;

import com.prafka.desktop.model.EncryptedStorageModel;
import com.prafka.desktop.model.TopicFilterTemplateModel;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TopicFilterTemplateServiceTest {

    private SessionService sessionService = mock(SessionService.class, RETURNS_DEEP_STUBS);
    private StorageService storageService = mock(StorageService.class);
    private TopicFilterTemplateService topicFilterTemplateService = new TopicFilterTemplateService(sessionService, storageService);

    @Test
    void shouldGetAll() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        var template1 = new TopicFilterTemplateModel();
        var template2 = new TopicFilterTemplateModel();
        encryptedStorage.getClusterTopicsFilterTemplates().put("cluster1", new HashMap<>(Map.of("topic1", new ArrayList<>(List.of(template1, template2)))));
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        // When
        var result = topicFilterTemplateService.getAll("topic1");

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void shouldGetDefault() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        var template1 = new TopicFilterTemplateModel();
        var template2 = new TopicFilterTemplateModel();
        template2.setByDefault(true);
        encryptedStorage.getClusterTopicsFilterTemplates().put("cluster1", new HashMap<>(Map.of("topic1", new ArrayList<>(List.of(template1, template2)))));
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        // When
        var result = topicFilterTemplateService.getDefault("topic1");

        // Then
        assertTrue(result.isPresent());
        assertEquals(template2.getId(), result.get().getId());
    }

    @Test
    void shouldSave() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        // When
        topicFilterTemplateService.save("topic1", new TopicFilterTemplateModel());

        // Then
        assertEquals(1, encryptedStorage.getClusterTopicsFilterTemplates().get("cluster1").get("topic1").size());
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldSaveDefault() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        var template1 = new TopicFilterTemplateModel();
        var template2 = new TopicFilterTemplateModel();
        template2.setByDefault(true);
        encryptedStorage.getClusterTopicsFilterTemplates().put("cluster1", new HashMap<>(Map.of("topic1", new ArrayList<>(List.of(template1, template2)))));
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        // When
        topicFilterTemplateService.saveDefault("topic1", Optional.of(template1));

        // Then
        assertTrue(template1.isByDefault());
        assertFalse(template2.isByDefault());
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldDelete() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        var template1 = new TopicFilterTemplateModel();
        var template2 = new TopicFilterTemplateModel();
        encryptedStorage.getClusterTopicsFilterTemplates().put("cluster1", new HashMap<>(Map.of("topic1", new ArrayList<>(List.of(template1, template2)))));
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        // When
        topicFilterTemplateService.delete("topic1", template1);

        // Then
        assertEquals(1, encryptedStorage.getClusterTopicsFilterTemplates().get("cluster1").get("topic1").size());
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldImportTemplates() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        when(sessionService.getCluster().getId()).thenReturn("cluster1");

        var json = """
            {
              "version": "1.0.0",
              "filters": [
                {
                  "name": "filter1",
                  "byDefault": false,
                  "filter": {
                    "from": {
                        "type": "BEGIN"
                    },
                    "maxResults": 100,
                    "partitions": [],
                    "keySerde": "STRING",
                    "valueSerde": "STRING",
                    "expressions": []
                  }
                }
              ]
            }
            """;

        // When
        topicFilterTemplateService.importTemplates("topic1", json);

        // Then
        assertEquals(1, encryptedStorage.getClusterTopicsFilterTemplates().get("cluster1").get("topic1").size());
        verify(storageService).saveEncryptedStorage();
    }
}