package com.prafka.desktop.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Manages the application onboarding process for first-time users.
 *
 * <p>Tracks whether onboarding has been completed and provides methods
 * to mark the onboarding as finished.
 */
@Singleton
public class OnboardingService {

    private final StorageService storageService;

    @Inject
    public OnboardingService(StorageService storageService) {
        this.storageService = storageService;
    }

    public boolean onboardingCompleted() {
        return storageService.getPlainStorage().isOnboardingCompleted();
    }

    public void completeOnboarding() {
        storageService.getPlainStorage().setOnboardingCompleted(true);
        storageService.savePlainStorage();
    }
}
