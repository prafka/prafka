package com.prafka.desktop.service;

import com.prafka.desktop.model.PlainStorageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private BackendClient backendClient = mock(BackendClient.class);
    private PlainStorageModel plainStorage;

    @BeforeEach
    void setUp() {
        plainStorage = spy(PlainStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);
    }

    @Test
    void shouldNotQueueEventWhenAnalyticsDisabled() {
        // Given
        plainStorage.setCollectAnalytics(false);
        var service = new AnalyticsServiceTestable(storageService, backendClient);

        // When
        service.uncaughtException(new RuntimeException("test"));

        // Then - no exception thrown, event should be ignored
        verifyNoInteractions(backendClient);
    }

    @Test
    void shouldCollectAnalyticsUpdateStorage() {
        // Given
        var service = new AnalyticsServiceTestable(storageService, backendClient);

        // When
        service.collectAnalytics(true);

        // Then
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldCollectAnalyticsSetValueInStorage() {
        // Given
        var service = new AnalyticsServiceTestable(storageService, backendClient);

        // When
        service.collectAnalytics(false);

        // Then
        verify(plainStorage).setCollectAnalytics(false);
    }

    /**
     * Testable subclass that doesn't start the scheduled service in constructor
     */
    static class AnalyticsServiceTestable extends AnalyticsService {

        AnalyticsServiceTestable(StorageService storageService, BackendClient backendClient) {
            super(storageService, backendClient);
        }

        @Override
        protected void startScheduler() {
            // Don't start scheduler in tests
        }
    }
}
