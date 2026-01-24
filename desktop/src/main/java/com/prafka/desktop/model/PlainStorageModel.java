package com.prafka.desktop.model;

import com.prafka.desktop.ApplicationProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Model for unencrypted persistent storage containing non-sensitive settings.
 *
 * <p>Stores user preferences (locale, theme, timestamp format), onboarding status,
 * analytics consent, and window size preferences.
 */
@Getter
@Setter
public class PlainStorageModel {

    private static final List<Locale> AVAILABLE_LOCALES = List.of(Locale.ENGLISH);
    private static final List<ThemeModel> AVAILABLE_THEMES = List.of(ThemeModel.LIGHT, ThemeModel.DARK);
    private static final List<TimestampFormatModel> AVAILABLE_TIMESTAMP_FORMATS = List.of(TimestampFormatModel.YYYY_MM_DD, TimestampFormatModel.DD_MM_YYYY);

    private String version = ApplicationProperties.VERSION;
    private String userId = UUID.randomUUID().toString();
    private String locale = (AVAILABLE_LOCALES.contains(Locale.getDefault()) ? Locale.getDefault() : Locale.ENGLISH).toLanguageTag();
    private String theme = ThemeModel.LIGHT.getCode();
    private String timestampFormat = TimestampFormatModel.YYYY_MM_DD.getCode();
    private LogModel log = new LogModel();
    private String masterPassword;
    private boolean onboardingCompleted;
    private boolean collectAnalytics = true;
    private Map<String, StageModel> stages = new HashMap<>();

    public List<Locale> getAvailableLocales() {
        return AVAILABLE_LOCALES;
    }

    public List<ThemeModel> getAvailableThemes() {
        return AVAILABLE_THEMES;
    }

    public List<TimestampFormatModel> getAvailableTimestampFormats() {
        return AVAILABLE_TIMESTAMP_FORMATS;
    }
}
