package com.prafka.desktop.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ResourceBundle;

@Singleton
public class I18nService {

    private final SettingsService settingsService;
    private ResourceBundle bundle;

    @Inject
    public I18nService(SettingsService settingsService) {
        this.settingsService = settingsService;
        loadBundle();
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void loadBundle() {
        bundle = ResourceBundle.getBundle("i18n/messages", settingsService.getLocale());
    }

    public String get(String key) {
        return bundle.getString(key);
    }
}
