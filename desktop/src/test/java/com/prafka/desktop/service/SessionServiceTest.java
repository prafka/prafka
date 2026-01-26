package com.prafka.desktop.service;

import com.prafka.desktop.model.ClusterModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void shouldGetMasterPasswordThrowWhenNotSet() {
        assertThrows(IllegalStateException.class, () -> sessionService.getMasterPassword());
    }

    @Test
    void shouldSetMasterPasswordHashWithSha512() {
        // Given
        when(cryptoService.hashPasswordForSession("test-password")).thenReturn("hashed-password");

        // When
        sessionService.setMasterPassword("test-password");
        var result = sessionService.getMasterPassword();

        // Then
        verify(cryptoService).hashPasswordForSession("test-password");
        assertArrayEquals("hashed-password".toCharArray(), result);
    }

    @Test
    void shouldGetClusterOptReturnEmptyWhenNotSet() {
        // When
        var result = sessionService.getClusterOpt();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetClusterOptReturnPresentWhenSet() {
        // Given
        var cluster = new ClusterModel();
        sessionService.setCluster(cluster);

        // When
        var result = sessionService.getClusterOpt();

        // Then
        assertTrue(result.isPresent());
        assertEquals(cluster.getId(), result.get().getId());
    }

    @Test
    void shouldGetClusterThrowWhenNotSet() {
        assertThrows(java.util.NoSuchElementException.class, () -> sessionService.getCluster());
    }

    @Test
    void shouldSetClusterUpdateOptional() {
        // Given
        assertTrue(sessionService.getClusterOpt().isEmpty());

        // When
        sessionService.setCluster(new ClusterModel());

        // Then
        assertTrue(sessionService.getClusterOpt().isPresent());
    }

    @Test
    void shouldSetMasterPasswordStoreAsCharArray() {
        // Given
        when(cryptoService.hashPasswordForSession(anyString())).thenReturn("hash");

        // When
        sessionService.setMasterPassword("password");

        // Then
        assertNotNull(sessionService.getMasterPassword());
        assertTrue(sessionService.getMasterPassword() instanceof char[]);
    }
}