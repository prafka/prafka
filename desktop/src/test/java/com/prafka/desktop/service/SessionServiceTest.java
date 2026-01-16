package com.prafka.desktop.service;

import com.prafka.desktop.model.ClusterModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionServiceTest {

    private CryptoService cryptoService = mock(CryptoService.class);
    private SessionService sessionService = new SessionService(cryptoService);

    @Test
    void shouldSetAndGetCluster() {
        // Given
        var cluster = new ClusterModel();

        // When
        sessionService.setCluster(cluster);
        var result = sessionService.getCluster();

        // Then
        assertEquals(cluster.getId(), result.getId());
    }
}