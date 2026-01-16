package com.prafka.desktop.service;

import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.model.EncryptedStorageModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClusterServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private ClusterService clusterService = new ClusterService(storageService);

    @Test
    void shouldSaveCluster() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);

        // When
        clusterService.saveCluster(new ClusterModel());

        // Then
        assertThat(encryptedStorage.getClusters()).hasSize(1);
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldSaveCurrentCluster() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        var cluster1 = new ClusterModel();
        var cluster2 = new ClusterModel();
        encryptedStorage.getClusters().addAll(List.of(cluster1, cluster2));

        // When
        clusterService.saveCurrentCluster(cluster2);

        // Then
        assertFalse(encryptedStorage.getClusters().get(0).isCurrent());
        assertTrue(encryptedStorage.getClusters().get(1).isCurrent());
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldDeleteCluster() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        var cluster1 = new ClusterModel();
        var cluster2 = new ClusterModel();
        encryptedStorage.getClusters().addAll(List.of(cluster1, cluster2));

        // When
        clusterService.deleteCluster(cluster1);

        // Then
        assertThat(encryptedStorage.getClusters()).hasSize(1);
        assertEquals(cluster2.getId(), encryptedStorage.getClusters().get(0).getId());
        verify(storageService).saveEncryptedStorage();
    }

    @Test
    void shouldImportCluster() {
        // Given
        var encryptedStorage = spy(EncryptedStorageModel.class);
        when(storageService.getEncryptedStorage()).thenReturn(encryptedStorage);
        encryptedStorage.getClusters().add(new ClusterModel());

        // When
        clusterService.importClusters("""
                {
                    "version": "1.0.0",
                    "clusters": [
                        {
                            "name": "cluster2",
                            "bootstrapServers": "localhost:9092",
                            "authenticationMethod": "NONE"
                        }
                    ]
                }
                """);

        // Then
        assertThat(encryptedStorage.getClusters()).hasSize(2);
        assertEquals("cluster2", encryptedStorage.getClusters().get(1).getName());
        verify(storageService).saveEncryptedStorage();
    }
}