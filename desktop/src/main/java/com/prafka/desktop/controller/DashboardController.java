package com.prafka.desktop.controller;

import com.prafka.core.manager.KafkaManager;
import com.prafka.core.manager.SerDeManager;
import com.prafka.core.service.HealthCheckService;
import com.prafka.desktop.concurrent.ScheduledServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ClusterService;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.service.ProxyService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class DashboardController extends AbstractController {

    public BorderPane borderPane;
    public ComboBox<ClusterModel> comboBoxSelectCluster;
    public Circle circleConnectionStatus;
    public Button buttonOverview;
    public Button buttonTopics;
    public Button buttonSchemaRegistry;
    public Button buttonConsumerGroups;
    public Button buttonKafkaConnect;
    public Button buttonBrokers;
    public Button buttonAcl;
    public Button buttonQuotas;
    public Button buttonClusters;
    //    public Button buttonCertificates;
    public Button buttonSettings;
    public Button buttonHelp;

    private final KafkaManager kafkaManager;
    private final SerDeManager serDeManager;
    private final ProxyService proxyService;
    private final ClusterService clusterService;
    private final HealthCheckService healthCheckService;
    private Optional<Stage> stage = Optional.empty();
    private ScheduledServiceAdapter<HealthCheckService.HealthCheckResult> healthCheckScheduler;

    @Inject
    public DashboardController(KafkaManager kafkaManager, SerDeManager serDeManager, ProxyService proxyService, ClusterService clusterService, HealthCheckService healthCheckService) {
        this.kafkaManager = kafkaManager;
        this.serDeManager = serDeManager;
        this.proxyService = proxyService;
        this.clusterService = clusterService;
        this.healthCheckService = healthCheckService;
    }

    public void setStage(Stage stage) {
        this.stage = Optional.of(stage);
    }

    @Override
    public void initFxml() {
        proxyService.init();

        var clusterIndex = -1;
        for (int i = 0; i < clusters().size(); i++) {
            if (clusters().get(i).isCurrent()) {
                clusterIndex = i;
                break;
            }
        }
        if (clusterIndex > -1) {
            sessionService.setCluster(clusters().get(clusterIndex));
            scheduleClusterHealthCheck();
        }
        comboBoxSelectCluster.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClusterModel cluster) {
                return cluster.getName();
            }

            @Override
            public ClusterModel fromString(String string) {
                return null;
            }
        });
        comboBoxSelectCluster.getItems().setAll(clusters());
        comboBoxSelectCluster.getSelectionModel().select(clusterIndex);
        comboBoxSelectCluster.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || (clusterOpt().isPresent() && cluster().getId().equals(newValue.getId()))) return;
            clusterOpt().ifPresent(it -> {
                var prevClusterId = it.getId();
                ServiceAdapter.task(() -> {
                    kafkaManager.close(prevClusterId);
                    serDeManager.close(prevClusterId);
                }).start();
            });
            clusterService.saveCurrentCluster(newValue);
            sessionService.setCluster(newValue);
            scheduleClusterHealthCheck();
            eventService.fire(EventService.DashboardEvent.LOAD_TOPICS);
        });

        eventService.register(new EventService.DashboardListener() {
            @Override
            public void onEvent(EventService.DashboardEvent event) {
                switch (event) {
                    case UPDATE_COMBOBOX_CLUSTERS -> {
                        var selectIndex = -1;
                        for (int i = 0; i < clusters().size(); i++) {
                            if (clusterOpt().isPresent() && cluster().getId().equals(clusters().get(i).getId())) {
                                selectIndex = i;
                                break;
                            } else if (clusters().get(i).isCurrent()) {
                                selectIndex = i;
                            }
                        }
                        comboBoxSelectCluster.getItems().setAll(clusters());
                        comboBoxSelectCluster.getSelectionModel().select(selectIndex);
                    }
                    case UPDATE_COMBOBOX_CLUSTERS_SILENT -> {
                        comboBoxSelectCluster.getItems().setAll(clusters());
                    }
                    case LOAD_OVERVIEW -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonOverview);
                        borderPane.setCenter(getView(it -> viewManager.loadClusterOverviewView()));
                        setTitle(i18nService.get("common.overview"));
                    }
                    case LOAD_TOPICS -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonTopics);
                        borderPane.setCenter(getView(it -> viewManager.loadTopicListView()));
                        setTitle(i18nService.get("common.topics"));
                    }
                    case LOAD_SCHEMA_REGISTRY -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonSchemaRegistry);
                        borderPane.setCenter(getView(cluster -> cluster.isSchemaRegistryDefined() ? viewManager.loadSchemaListView() : noSchemaRegistryView()));
                        setTitle(i18nService.get("common.schemaRegistry"));
                    }
                    case LOAD_CONSUMER_GROUPS -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonConsumerGroups);
                        borderPane.setCenter(getView(it -> viewManager.loadConsumerGroupListView()));
                        setTitle(i18nService.get("common.consumerGroups"));
                    }
                    case LOAD_KAFKA_CONNECT -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonKafkaConnect);
                        borderPane.setCenter(getView(cluster -> cluster.isConnectsDefined() ? viewManager.loadConnectorListView() : noKafkaConnectView()));
                        setTitle(i18nService.get("common.kafkaConnect"));
                    }
                    case LOAD_BROKERS -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonBrokers);
                        borderPane.setCenter(getView(it -> viewManager.loadBrokerListView()));
                        setTitle(i18nService.get("common.brokers"));
                    }
                    case LOAD_ACL -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonAcl);
                        borderPane.setCenter(getView(it -> viewManager.loadAclListView()));
                        setTitle(i18nService.get("common.acl"));
                    }
                    case LOAD_QUOTAS -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonQuotas);
                        borderPane.setCenter(getView(it -> viewManager.loadQuotaListView()));
                        setTitle(i18nService.get("common.quotas"));
                    }
                    case LOAD_CLUSTERS -> {
                        unsetCurrentButton();
                        setCurrentButton(buttonClusters);
                        borderPane.setCenter(viewManager.loadClusterListView());
                        setTitle(i18nService.get("common.clusters"));
                    }
//                    case LOAD_CERTIFICATES -> {
//                        unsetCurrentButton();
//                        setCurrentButton(buttonCertificates));
//                        borderPane.setCenter(new Label("Certificates"));
//                    }
                }
            }

            @Override
            public void onEvent(EventService.DashboardContentEvent event) {
                borderPane.setCenter(event.content());
            }
        });

        themeService.setIcon16(buttonOverview, "pulse.png");
        buttonOverview.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_OVERVIEW));

        themeService.setIcon16(buttonTopics, "albums.png");
        buttonTopics.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_TOPICS));

        themeService.setIcon16(buttonSchemaRegistry, "documents.png");
        buttonSchemaRegistry.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_SCHEMA_REGISTRY));

        themeService.setIcon16(buttonConsumerGroups, "copy.png");
        buttonConsumerGroups.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_CONSUMER_GROUPS));

        themeService.setIcon16(buttonKafkaConnect, "link.png");
        buttonKafkaConnect.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_KAFKA_CONNECT));

        themeService.setIcon16(buttonBrokers, "server.png");
        buttonBrokers.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_BROKERS));

        themeService.setIcon16(buttonAcl, "key.png");
        buttonAcl.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_ACL));

        themeService.setIcon16(buttonQuotas, "speedometer.png");
        buttonQuotas.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_QUOTAS));

        themeService.setIcon16(buttonClusters, "hardware_chip.png");
        buttonClusters.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_CLUSTERS));

//        themeService.setIcon16(buttonCertificates, "document_text.png");
//        buttonCertificates.setOnAction(it -> eventService.fire(EventService.DashboardEvent.LOAD_CERTIFICATES));

        themeService.setIcon16(buttonSettings, "settings.png");
        buttonSettings.setOnAction(it -> viewManager.showSettingsView(JavaFXUtils.getStage(it)));

        themeService.setIcon16(buttonHelp, "help.png");
        buttonHelp.setOnAction(it -> viewManager.showHelpView(JavaFXUtils.getStage(it)));

        eventService.fire(EventService.DashboardEvent.LOAD_TOPICS);
    }

    @Override
    public void close() {
        super.close();
        if (healthCheckScheduler != null) {
            healthCheckScheduler.cancel();
        }
    }

    private Node getView(Function<ClusterModel, Node> onClusterExists) {
        if (CollectionUtils.isEmpty(clusters())) return noClusterView();
        return clusterOpt().map(onClusterExists).orElseGet(this::noSelectClusterView);
    }

    private Node noClusterView() {
        var box = new VBox();
        box.setSpacing(20);
        box.setAlignment(Pos.CENTER);
        var label = new Label(i18nService.get("dashboardView.clusterIsNotConfigured"));
        label.getStyleClass().add("font-medium");
        var button = new Button(i18nService.get("dashboardView.addCluster"));
        button.getStyleClass().add("primary");
        button.setOnAction(actionEvent ->
                viewManager.showAddClusterView(JavaFXUtils.getStage(actionEvent), () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterAdded"));
                    eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                })
        );
        box.getChildren().addAll(label, button);
        return box;
    }

    private Node noSelectClusterView() {
        var box = new VBox();
        box.setAlignment(Pos.CENTER);
        var label = new Label(i18nService.get("dashboardView.selectCluster"));
        label.getStyleClass().add("font-medium");
        box.getChildren().addAll(label);
        return box;
    }

    private Node noSchemaRegistryView() {
        var box = new VBox();
        box.setSpacing(20);
        box.setAlignment(Pos.CENTER);
        var label = new Label(i18nService.get("dashboardView.schemaRegistryIsNotConfigured"));
        label.getStyleClass().add("font-medium");
        var button = new Button(i18nService.get("dashboardView.addSchemaRegistry"));
        button.getStyleClass().add("primary");
        button.setOnAction(actionEvent ->
                viewManager.showEditClusterView(JavaFXUtils.getStage(actionEvent), cluster(), 1, () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterUpdated"));
                    eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                    eventService.fire(EventService.DashboardEvent.LOAD_SCHEMA_REGISTRY);
                })
        );
        box.getChildren().addAll(label, button);
        return box;
    }

    private Node noKafkaConnectView() {
        var box = new VBox();
        box.setSpacing(20);
        box.setAlignment(Pos.CENTER);
        var label = new Label(i18nService.get("dashboardView.kafkaConnectIsNotConfigured"));
        label.getStyleClass().add("font-medium");
        var button = new Button(i18nService.get("dashboardView.addKafkaConnect"));
        button.getStyleClass().add("primary");
        button.setOnAction(actionEvent ->
                viewManager.showEditClusterView(JavaFXUtils.getStage(actionEvent), cluster(), 2, () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterUpdated"));
                    eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                    eventService.fire(EventService.DashboardEvent.LOAD_KAFKA_CONNECT);
                })
        );
        box.getChildren().addAll(label, button);
        return box;
    }

    private void setCurrentButton(Button button) {
        button.getStyleClass().add("current");
    }

    private void unsetCurrentButton(Button button) {
        button.getStyleClass().remove("current");
    }

    private void unsetCurrentButton() {
        unsetCurrentButton(buttonOverview);
        unsetCurrentButton(buttonTopics);
        unsetCurrentButton(buttonSchemaRegistry);
        unsetCurrentButton(buttonConsumerGroups);
        unsetCurrentButton(buttonKafkaConnect);
        unsetCurrentButton(buttonBrokers);
        unsetCurrentButton(buttonAcl);
        unsetCurrentButton(buttonQuotas);
        unsetCurrentButton(buttonClusters);
//        unsetCurrentButton(buttonCertificates);
    }

    private void setTitle(String title) {
        stage.ifPresent(it -> Platform.runLater(() -> viewManager.setStageTitleWithAppName(it, title)));
    }

    private void scheduleClusterHealthCheck() {
        if (this.healthCheckScheduler != null) {
            this.healthCheckScheduler.cancel();
        }
        this.healthCheckScheduler = ScheduledServiceAdapter.scheduleTask(() -> healthCheckService.isAvailable(clusterId()).get())
                .onSuccess(result -> {
                    var styleClass = result.cluster().available() ? "shape-success" : "shape-error";
                    if (result.cluster().available()) {
                        if (result.schemaRegistry().isPresent() && !result.schemaRegistry().get().available()) {
                            styleClass = "shape-warn";
                        }
                        if (result.connects().isPresent() && !result.connects().get().values().stream().allMatch(HealthCheckService.HealthCheckResult.Item::available)) {
                            styleClass = "shape-warn";
                        }
                    }
                    circleConnectionStatus.getStyleClass().setAll(styleClass);
                })
                .onError(it -> {
                    circleConnectionStatus.getStyleClass().setAll("shape-error");
                })
                .start(Duration.seconds(5));
    }

    private List<ClusterModel> clusters() {
        return clusterService.getClusters();
    }

    private Optional<ClusterModel> clusterOpt() {
        return sessionService.getClusterOpt();
    }

    private ClusterModel cluster() {
        return sessionService.getCluster();
    }
}
