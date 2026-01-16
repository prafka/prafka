package com.prafka.desktop.controller.connect;

import com.prafka.core.model.Connector;
import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.util.concurrent.atomic.AtomicReference;

import static com.prafka.core.util.StreamUtils.tryIgnore;

public class ConnectorController extends AbstractController {

    public Label labelH1;
    public Label labelH1Comeback;
    public Button buttonPause;
    public Button buttonResume;
    public Button buttonRestart;
    public Button buttonDelete;
    public MenuButton buttonMore;
    public Label labelCardWorkerTitle;
    public Pane paneCardWorkerContent;
    public Label labelCardPluginTitle;
    public Pane paneCardPluginContent;
    public Label labelCardTypeTitle;
    public Pane paneCardTypeContent;
    public Label labelCardStateTitle;
    public Pane paneCardStateContent;
    public Label labelCardRunTaskCountTitle;
    public Pane paneCardRunTaskCountContent;
    public Label labelCardFailTaskCountTitle;
    public Pane paneCardFailTaskCountContent;
    public TabPane tabPane;
    public Tab tabTasks;
    public Tab tabConfiguration;

    private final ConnectService connectService;
    private final AtomicReference<Runnable> silentLoadTasks = new AtomicReference<>();
    private Connector.Name cn;

    @Inject
    public ConnectorController(ConnectService connectService) {
        this.connectService = connectService;
    }

    public void setCn(Connector.Name cn) {
        this.cn = cn;
    }

    @Override
    public void initFxml() {
        labelH1Comeback.setOnMouseClicked(it -> eventService.fire(EventService.DashboardEvent.LOAD_KAFKA_CONNECT));

        initializeButtons();

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (disableLoadData) return;
            if (tabTasks.getId().equals(newValue.getId())) {
                tabTasks.setContent(viewManager.loadConnectorTabTasksView(cn, silentLoadTasks));
                return;
            }
            if (tabConfiguration.getId().equals(newValue.getId())) {
                tabConfiguration.setContent(viewManager.loadConnectorTabConfigurationView(cn));
                return;
            }
        });
    }

    @Override
    public void initUi() {
        labelH1.setText(cn.name());
        tabPane.getSelectionModel().select(0);
    }

    @Override
    public void initData() {
        Platform.runLater(() -> tabTasks.setContent(viewManager.loadConnectorTabTasksView(cn, silentLoadTasks)));
        loadSummary();
    }

    private void initializeButtons() {
        Runnable update = () ->
                ServiceAdapter.task(() -> tryIgnore(() -> Thread.sleep(500))) // todo make better
                        .onSuccess(it -> {
                            FutureServiceAdapter.futureTask(() -> connectService.getConnectorSummary(clusterId(), cn))
                                    .onSuccess(summary -> {
                                        JavaFXUtils.setLabel(JavaFXUtils.label(summary.state().name()), paneCardStateContent);
                                        JavaFXUtils.setLabel(JavaFXUtils.label(summary.runTaskCount()), paneCardRunTaskCountContent);
                                        JavaFXUtils.setLabel(JavaFXUtils.label(summary.failTaskCount()), paneCardFailTaskCountContent);
                                        buttonPause.setVisible(summary.state() == Connector.State.RUNNING);
                                        buttonResume.setVisible(summary.state() == Connector.State.PAUSED);
                                    })
                                    .start();
                            if (tabTasks.isSelected() && silentLoadTasks.get() != null) {
                                silentLoadTasks.get().run();
                            }
                        })
                        .start();

        buttonPause.setOnAction(actionEvent ->
                FutureServiceAdapter.futureTask(() -> connectService.pause(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorPaused"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start()
        );

        buttonResume.setOnAction(actionEvent ->
                FutureServiceAdapter.futureTask(() -> connectService.resume(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorResumed"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start()
        );

        buttonRestart.setOnAction(actionEvent ->
                FutureServiceAdapter.futureTask(() -> connectService.restart(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorRestarted"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start()
        );

        buttonDelete.setOnAction(actionEvent ->
                viewManager.showDeleteConnectorConfirmView(JavaFXUtils.getStage(actionEvent), confirmCallback ->
                        FutureServiceAdapter.futureTask(() -> connectService.delete(clusterId(), cn))
                                .onSuccess(it -> {
                                    confirmCallback.onSuccess();
                                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorDeleted"));
                                    eventService.fire(EventService.DashboardEvent.LOAD_KAFKA_CONNECT);
                                })
                                .onError(confirmCallback::onError)
                                .start()
                )
        );

        var menuItemRestartAllTasks = new MenuItem(i18nService.get("connect.restartAllTasks"));
        menuItemRestartAllTasks.setOnAction(actionEvent ->
                FutureServiceAdapter.futureTask(() -> connectService.restartAllTasks(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.allTasksRestarted"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start()
        );

        var menuItemRestartFailedTasks = new MenuItem(i18nService.get("connect.restartFailedTasks"));
        menuItemRestartFailedTasks.setOnAction(actionEvent ->
                FutureServiceAdapter.futureTask(() -> connectService.restartFailedTasks(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.failedTasksRestarted"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start()
        );

        themeService.setIcon(buttonMore, themeService.getIcon16("ellipsis_horizontal.png"));
        buttonMore.getItems().addAll(menuItemRestartAllTasks, menuItemRestartFailedTasks);
    }

    private void loadSummary() {
        JavaFXUtils.setPaneLoader(themeService.getIconLoader16(), paneCardWorkerContent, paneCardPluginContent, paneCardTypeContent, paneCardStateContent, paneCardRunTaskCountContent, paneCardFailTaskCountContent);
        FutureServiceAdapter.futureTask(() -> connectService.getConnectorSummary(clusterId(), cn))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.workerId()), paneCardWorkerContent);
                    JavaFXUtils.setLabel(JavaFXUtils.labelWithTooltip(summary.plugin().getClassShort(), JavaFXUtils.tooltip(summary.plugin().getClassFull())), paneCardPluginContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.type().name()), paneCardTypeContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.state().name()), paneCardStateContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.runTaskCount()), paneCardRunTaskCountContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.failTaskCount()), paneCardFailTaskCountContent);
                    buttonPause.setVisible(summary.state() == Connector.State.RUNNING);
                    buttonResume.setVisible(summary.state() == Connector.State.PAUSED);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneCardWorkerContent, paneCardPluginContent, paneCardTypeContent, paneCardStateContent, paneCardRunTaskCountContent, paneCardFailTaskCountContent);
                    loadDataError(it);
                })
                .start();
    }
}
