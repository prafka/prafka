package com.prafka.desktop.model;

import lombok.Getter;

/**
 * Enumeration of available application color themes.
 *
 * <p>Defines light and dark theme options with their corresponding codes and display names.
 */
@Getter
public enum ThemeModel {

    LIGHT("light", "common.themeLight"),
    DARK("dark", "common.themeDark");

    private final String code;
    private final String name;

    ThemeModel(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ThemeModel getByCode(String code) {
        return switch (code) {
            case "light" -> ThemeModel.LIGHT;
            case "dark" -> ThemeModel.DARK;
            default -> throw new IllegalArgumentException();
        };
    }
}
