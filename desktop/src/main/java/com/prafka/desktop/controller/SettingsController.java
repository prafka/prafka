package com.prafka.desktop.controller;

import com.prafka.desktop.model.LogModel;
import com.prafka.desktop.model.ProxyModel;
import com.prafka.desktop.model.ThemeModel;
import com.prafka.desktop.model.TimestampFormatModel;
import com.prafka.desktop.service.LogService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Locale;

import static com.prafka.core.util.StreamUtils.tryOrEmpty;

public class SettingsController extends AbstractController {

    public Pane paneContent;
    public ComboBox<Locale> comboBoxLanguage;
    public ComboBox<ThemeModel> comboBoxTheme;
    public ComboBox<TimestampFormatModel> comboBoxTimestampFormat;
    public RadioButton radioButtonNoProxy;
    public RadioButton radioButtonManualProxy;
    public VBox paneManualProxy;
    public TextField textFieldProxyHost;
    public TextField textFieldProxyPort;
    public TextField textFieldProxyLogin;
    public TextField textFieldProxyPassword;
    public TextField textFieldLogDir;
    public CheckBox checkBoxLogDebug;
    public Pane paneAlert;
    public Button buttonCancel;
    public Button buttonSave;

    private final LogService logService;

    @Inject
    public SettingsController(LogService logService) {
        this.logService = logService;
    }

    @Override
    public void initFxml() {
        initInterfaceTab();
        initProxyTab();
        initLogsTab();

        buttonSave.setOnAction(actionEvent -> {
            var needRestart = needRestart();

            if (languageChanged()) {
                settingsService.saveLocale(comboBoxLanguage.getValue());
                i18nService.loadBundle();
                viewManager.clear();
            }

            if (themeChanged()) {
                settingsService.saveTheme(comboBoxTheme.getValue());
                viewManager.clear();
            }

            if (timestampFormatChanged()) {
                settingsService.saveTimestampFormat(comboBoxTimestampFormat.getValue());
            }

            if (proxyChanged()) {
                settingsService.saveProxy(createProxy());
            }

            if (logsChanged()) {
                var log = new LogModel();
                log.setDebug(checkBoxLogDebug.isSelected());
                settingsService.saveLog(log);
                if (log.isDebug()) {
                    logService.setDebug();
                } else {
                    logService.setInfo();
                }
            }

            var currentStage = JavaFXUtils.getStage(actionEvent);
            var dashboardStage = (Stage) currentStage.getOwner();
            if (needRestart) {
                viewManager.showDashboardView();
                currentStage.close();
                dashboardStage.close();
            } else {
                currentStage.close();
            }
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    protected void onEnter() {
        buttonSave.fire();
    }

    private void initInterfaceTab() {
        comboBoxLanguage.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxLanguage.getItems().addAll(settingsService.getAvailableLocales());
        comboBoxLanguage.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                return StringUtils.capitalize(locale.getDisplayLanguage(settingsService.getLocale()));
            }

            @Override
            public Locale fromString(String string) {
                return null;
            }
        });
        comboBoxLanguage.getSelectionModel().select(settingsService.getLocale());
        comboBoxLanguage.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());

        comboBoxTheme.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxTheme.getItems().addAll(settingsService.getAvailableThemes());
        comboBoxTheme.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeModel theme) {
                return i18nService.get(theme.getName());
            }

            @Override
            public ThemeModel fromString(String string) {
                return null;
            }
        });
        comboBoxTheme.getSelectionModel().select(settingsService.getTheme());
        comboBoxTheme.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());

        comboBoxTimestampFormat.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxTimestampFormat.getItems().addAll(settingsService.getAvailableTimestampFormats());
        comboBoxTimestampFormat.setConverter(new StringConverter<>() {
            @Override
            public String toString(TimestampFormatModel timestampFormat) {
                return timestampFormat.getShortPattern();
            }

            @Override
            public TimestampFormatModel fromString(String string) {
                return null;
            }
        });
        comboBoxTimestampFormat.getSelectionModel().select(settingsService.getTimestampFormat());
        comboBoxTimestampFormat.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
    }

    private void initProxyTab() {
        paneManualProxy.disableProperty().bind(radioButtonManualProxy.selectedProperty().map(it -> !it));
        textFieldProxyPort.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(null));

        var proxy = settingsService.getProxy();
        if (proxy != null) {
            switch (proxy.getType()) {
                case NO -> radioButtonNoProxy.setSelected(true);
                case MANUAL -> radioButtonManualProxy.setSelected(true);
            }
            textFieldProxyHost.setText(proxy.getHost());
            if (proxy.getPort() != null) textFieldProxyPort.setText(proxy.getPort().toString());
            textFieldProxyLogin.setText(proxy.getUser());
            textFieldProxyPassword.setText(proxy.getPassword());
        }

        radioButtonNoProxy.selectedProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
        radioButtonManualProxy.selectedProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
        textFieldProxyHost.textProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
        textFieldProxyPort.textProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
        textFieldProxyLogin.textProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
        textFieldProxyPassword.textProperty().addListener((observable, oldValue, newValue) -> alertAppWillRestart());
    }

    private void initLogsTab() {
        textFieldLogDir.setText(logService.getDir());
        checkBoxLogDebug.setSelected(settingsService.getLog().isDebug());
    }

    private boolean languageChanged() {
        return comboBoxLanguage.getValue() != settingsService.getLocale();
    }

    private boolean themeChanged() {
        return comboBoxTheme.getValue() != settingsService.getTheme();
    }

    private boolean timestampFormatChanged() {
        return comboBoxTimestampFormat.getValue() != settingsService.getTimestampFormat();
    }

    private boolean logsChanged() {
        return checkBoxLogDebug.isSelected() != settingsService.getLog().isDebug();
    }

    private boolean proxyChanged() {
        var newProxy = createProxy();
        var currentProxy = settingsService.getProxy();
        if (newProxy.getType() != currentProxy.getType()) return true;
        if (newProxy.getType() == ProxyModel.Type.MANUAL && currentProxy.getType() == ProxyModel.Type.MANUAL) {
            return stringChanged(newProxy.getHost(), currentProxy.getHost())
                    || integerChanged(newProxy.getPort(), currentProxy.getPort())
                    || stringChanged(newProxy.getUser(), currentProxy.getUser())
                    || stringChanged(newProxy.getPassword(), currentProxy.getPassword());
        }
        return false;
    }

    private boolean stringChanged(String s1, String s2) {
        if (StringUtils.isBlank(s1) && StringUtils.isBlank(s2)) return false;
        if (StringUtils.isBlank(s1) && StringUtils.isNotBlank(s2)) return true;
        if (StringUtils.isNotBlank(s1) && StringUtils.isBlank(s2)) return true;
        return !Strings.CS.equals(s1, s2);
    }

    private boolean integerChanged(Integer i1, Integer i2) {
        if (i1 == null && i2 == null) return false;
        if (i1 == null || i2 == null) return true;
        return !i1.equals(i2);
    }

    private boolean needRestart() {
        return languageChanged() || themeChanged() || timestampFormatChanged() || proxyChanged();
    }

    private ProxyModel createProxy() {
        var type = ProxyModel.Type.NO;
        if (radioButtonManualProxy.isSelected()) type = ProxyModel.Type.MANUAL;
        var proxy = new ProxyModel();
        proxy.setType(type);
        if (type == ProxyModel.Type.MANUAL) {
            proxy.setHost(textFieldProxyHost.getText());
            tryOrEmpty(() -> Integer.parseInt(textFieldProxyPort.getText())).ifPresent(proxy::setPort);
            proxy.setUser(textFieldProxyLogin.getText());
            proxy.setPassword(textFieldProxyPassword.getText());
        }
        return proxy;
    }

    private void alertAppWillRestart() {
        paneAlert.getChildren().clear();
        if (needRestart()) {
            var label = new Label(i18nService.get("settingsView.appWillRestart"));
            label.getStyleClass().add("font-gray");
            paneAlert.getChildren().add(label);
        }
    }
}
