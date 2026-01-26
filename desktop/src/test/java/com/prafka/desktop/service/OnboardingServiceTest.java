package com.prafka.desktop.service;

import com.prafka.desktop.model.PlainStorageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnboardingServiceTest {

    private StorageService storageService = mock(StorageService.class);
    private OnboardingService onboardingService;
    private PlainStorageModel plainStorage;

    @BeforeEach
    void setUp() {
        plainStorage = spy(PlainStorageModel.class);
        when(storageService.getPlainStorage()).thenReturn(plainStorage);
        onboardingService = new OnboardingService(storageService);
    }

    @Test
    void shouldCompleteOnboarding() {
        // When
        onboardingService.completeOnboarding();

        // Then
        assertTrue(plainStorage.isOnboardingCompleted());
        verify(storageService).savePlainStorage();
    }

    @Test
    void shouldOnboardingCompletedReturnFalseInitially() {
        // Given
        plainStorage.setOnboardingCompleted(false);

        // When
        var result = onboardingService.onboardingCompleted();

        // Then
        assertFalse(result);
    }

    @Test
    void shouldOnboardingCompletedReturnTrueAfterCompletion() {
        // Given
        plainStorage.setOnboardingCompleted(true);

        // When
        var result = onboardingService.onboardingCompleted();

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCompleteOnboardingSaveStorage() {
        // When
        onboardingService.completeOnboarding();

        // Then
        verify(storageService).savePlainStorage();
    }
}