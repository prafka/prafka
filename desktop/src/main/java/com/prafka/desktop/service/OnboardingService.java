package com.prafka.desktop.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
