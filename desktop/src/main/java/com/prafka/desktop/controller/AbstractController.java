package com.prafka.desktop.controller;

import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.manager.ViewManager;
import com.prafka.desktop.service.*;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for all JavaFX controllers in the application.
 *
 * <p>Provides common services via dependency injection, lifecycle methods for UI
 * and data initialization, and utilities for error handling and task management.
 */
public abstract class AbstractController implements Initializable, Closeable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @FXML
    public Pane paneRoot;

    protected ViewManager viewManager;
    protected I18nService i18nService;
    protected SceneService sceneService;
    protected SessionService sessionService;
    protected ThemeService themeService;
    protected SettingsService settingsService;
    protected EventService eventService;
    protected boolean disableLoadData = true;
    protected final List<FutureServiceAdapter<?>> futureTasks = new CopyOnWriteArrayList<>();

    @Inject
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @Inject
    public void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @Inject
    public void setSceneService(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @Inject
    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Inject
    public void setThemeService(ThemeService themeService) {
        this.themeService = themeService;
    }

    @Inject
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    protected String clusterId() {
        return sessionService.getCluster().getId();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initFxml();
    }

    public void initFxml() {
    }

    public void initializeUi() {
        disableLoadData = true;
        initUi();
        disableLoadData = false;
    }

    public void initUi() {
    }

    public void initializeData() {
        initData();
    }

    public void initData() {
    }

    public void initializeScene(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::onEnter));
        initScene(scene);
    }

    public void initScene(Scene scene) {
    }

    @Override
    public void close() {
        JavaFXUtils.clearTasks(futureTasks);
    }

    protected void onEnter() {
    }

    protected void loadDataError(Throwable throwable) {
        loadDataError(Pos.BOTTOM_RIGHT, throwable);
    }

    protected void loadDataError(Pos pos, Throwable throwable) {
        loadDataError(JavaFXUtils.getStage(paneRoot), pos, throwable);
    }

    protected void loadDataError(Stage primaryStage, Pos pos, Throwable throwable) {
        sceneService.showSnackbarError(primaryStage, pos, i18nService.get("common.loadDataError"), throwable);
        log.error(i18nService.get("common.loadDataError"), throwable);
    }

    protected void logError(Throwable throwable) {
        log.error(i18nService.get("common.error"), throwable);
    }
}
