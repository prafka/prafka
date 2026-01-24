package com.prafka.desktop.service;

import com.prafka.desktop.model.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Locale;

/**
 * Manages user preferences and application settings.
 *
 * <p>Provides access to locale, theme, timestamp format, proxy configuration,
 * logging settings, and window size preferences.
 */
@Singleton
public class SettingsService {

    private final StorageService storageService;

    @Inject
    public SettingsService(StorageService storageService) {
        this.storageService = storageService;
        this.storageService.loadPlainStorage();
    }

    public List<Locale> getAvailableLocales() {
        return storageService.getPlainStorage().getAvailableLocales();
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(storageService.getPlainStorage().getLocale());
    }

    public void saveLocale(Locale locale) {
        if (storageService.getPlainStorage().getLocale().equals(locale.toLanguageTag())) return;
        storageService.getPlainStorage().setLocale(locale.toLanguageTag());
        storageService.savePlainStorage();
    }

    public List<ThemeModel> getAvailableThemes() {
        return storageService.getPlainStorage().getAvailableThemes();
    }

    public ThemeModel getTheme() {
        return ThemeModel.getByCode(storageService.getPlainStorage().getTheme());
    }

    public void saveTheme(ThemeModel theme) {
        if (storageService.getPlainStorage().getTheme().equals(theme.getCode())) return;
        storageService.getPlainStorage().setTheme(theme.getCode());
        storageService.savePlainStorage();
    }

    public List<TimestampFormatModel> getAvailableTimestampFormats() {
        return storageService.getPlainStorage().getAvailableTimestampFormats();
    }

    public TimestampFormatModel getTimestampFormat() {
        return TimestampFormatModel.getByCode(storageService.getPlainStorage().getTimestampFormat());
    }

    public void saveTimestampFormat(TimestampFormatModel timestampFormat) {
        if (storageService.getPlainStorage().getTimestampFormat().equals(timestampFormat.getCode())) return;
        storageService.getPlainStorage().setTimestampFormat(timestampFormat.getCode());
        storageService.savePlainStorage();
    }

    public ProxyModel getProxy() {
        return storageService.getEncryptedStorage().getProxy();
    }

    public void saveProxy(ProxyModel proxy) {
        storageService.getEncryptedStorage().setProxy(proxy);
        storageService.saveEncryptedStorage();
    }

    public LogModel getLog() {
        return storageService.getPlainStorage().getLog();
    }

    public void saveLog(LogModel log) {
        storageService.getPlainStorage().setLog(log);
        storageService.savePlainStorage();
    }

    public int getStageWidth(String id, int defaultValue) {
        return (getStageSettings(id).getSize().getWidth() == null) ? defaultValue : getStageSettings(id).getSize().getWidth();
    }

    public void saveStageWidth(String id, int width) {
        getStageSettings(id).getSize().setWidth(width);
        storageService.savePlainStorage();
    }

    public int getStageHeight(String id, int defaultValue) {
        return (getStageSettings(id).getSize().getHeight() == null) ? defaultValue : getStageSettings(id).getSize().getHeight();
    }

    public void saveStageHeight(String id, int height) {
        getStageSettings(id).getSize().setHeight(height);
        storageService.savePlainStorage();
    }

    private StageModel getStageSettings(String id) {
        return storageService.getPlainStorage().getStages().computeIfAbsent(id, key -> new StageModel());
    }
}
