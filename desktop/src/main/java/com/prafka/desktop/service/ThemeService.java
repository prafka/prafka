package com.prafka.desktop.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ThemeService {

    public static final List<Image> ICONS = List.of(
            new Image("/img/icon16.png"),
            new Image("/img/icon32.png"),
            new Image("/img/icon64.png"),
            new Image("/img/icon128.png"),
            new Image("/img/icon256.png")
    );

    private final SettingsService settingsService;
    private final String javafxDefaultStyle;
    private final String javafxLightStyle;
    private final String javafxDarkStyle;
    private final String appDefaultStyle;
    private final String appLightStyle;
    private final String appDarkStyle;
    private final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    @Inject
    public ThemeService(SettingsService settingsService) {
        this.settingsService = settingsService;
        javafxDefaultStyle = getClass().getResource("/css/javafx-default.css").toExternalForm();
        javafxLightStyle = getClass().getResource("/css/javafx-light.css").toExternalForm();
        javafxDarkStyle = getClass().getResource("/css/javafx-dark.css").toExternalForm();
        appDefaultStyle = getClass().getResource("/css/app-default.css").toExternalForm();
        appLightStyle = getClass().getResource("/css/app-light.css").toExternalForm();
        appDarkStyle = getClass().getResource("/css/app-dark.css").toExternalForm();
    }

    public void addStylesheets(Scene scene) {
        scene.getStylesheets().addAll(javafxDefaultStyle, appDefaultStyle);
        switch (settingsService.getTheme()) {
            case LIGHT -> scene.getStylesheets().addAll(javafxLightStyle, appLightStyle);
            case DARK -> scene.getStylesheets().addAll(javafxDarkStyle, appDarkStyle);
        }
    }

    public Image getIconLoader16() {
        return getIcon16("loader.gif");
    }

    public Image getIcon16(String icon) {
        return getIcon("16/" + icon);
    }

    public Image getIcon20(String icon) {
        return getIcon("20/" + icon);
    }

    public Image getIcon(String icon) {
        var resource = switch (settingsService.getTheme()) {
            case LIGHT -> "/icon/light/" + icon;
            case DARK -> "/icon/dark/" + icon;
        };
        return IMAGE_CACHE.computeIfAbsent(resource, key -> new Image(getClass().getResource(resource).toExternalForm()));
    }

    public void setIcon16(Labeled labeled, String icon) {
        setIcon(labeled, "16/" + icon);
    }

    public void setIcon20(Labeled labeled, String icon) {
        setIcon(labeled, "20/" + icon);
    }

    public void setIcon(Labeled labeled, String icon) {
        setIcon(labeled, getIcon(icon));
    }

    public void setIcon(Labeled labeled, Image icon) {
        labeled.setGraphic(new ImageView(icon));
    }
}
