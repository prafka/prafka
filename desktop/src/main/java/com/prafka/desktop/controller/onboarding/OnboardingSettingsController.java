package com.prafka.desktop.controller.onboarding;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.model.ThemeModel;
import com.prafka.desktop.service.AnalyticsService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class OnboardingSettingsController extends AbstractController {

    public Pane paneContent;
    public ComboBox<Locale> comboBoxSelectLanguage;
    public ComboBox<ThemeModel> comboBoxSelectTheme;
    public CheckBox checkBoxCollectAnalytics;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonNext;

    private final AnalyticsService analyticsService;

    @Inject
    public OnboardingSettingsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public void initFxml() {
        comboBoxSelectLanguage.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxSelectLanguage.getItems().addAll(settingsService.getAvailableLocales());
        comboBoxSelectLanguage.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                return StringUtils.capitalize(locale.getDisplayLanguage(settingsService.getLocale()));
            }

            @Override
            public Locale fromString(String string) {
                return null;
            }
        });
        comboBoxSelectLanguage.getSelectionModel().select(settingsService.getLocale());

        comboBoxSelectTheme.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxSelectTheme.getItems().addAll(settingsService.getAvailableThemes());
        comboBoxSelectTheme.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeModel theme) {
                return i18nService.get(theme.getName());
            }

            @Override
            public ThemeModel fromString(String string) {
                return null;
            }
        });
        comboBoxSelectTheme.getSelectionModel().select(settingsService.getTheme());

        buttonNext.setOnAction(actionEvent -> {
            progressIndicator.setVisible(true);
            buttonNext.setDisable(true);
            task(() -> {
                settingsService.saveLocale(comboBoxSelectLanguage.getValue());
                i18nService.loadBundle();
                settingsService.saveTheme(comboBoxSelectTheme.getValue());
                analyticsService.collectAnalytics(checkBoxCollectAnalytics.isSelected());
            }).onSuccess(it -> viewManager.showOnboardingCreateMasterPasswordView(JavaFXUtils.getStage(actionEvent))).onError(this::logError).start();
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    protected void onEnter() {
        buttonNext.fire();
    }
}
