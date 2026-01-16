package com.prafka.desktop.service;

import com.prafka.desktop.model.PlainStorageModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private OnboardingService onboardingService = new OnboardingService(storageService);

    @Test
    void shouldCompleteOnboarding() {
        // Given
        var plainStorage = spy(PlainStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);

        // When
        onboardingService.completeOnboarding();

        // Then
        assertTrue(plainStorage.isOnboardingCompleted());
        verify(storageService).savePlainStorage();
    }
}