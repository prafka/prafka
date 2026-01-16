package com.prafka.desktop.controller.cluster;

import com.prafka.core.manager.KafkaManager;
import com.prafka.core.manager.SerDeManager;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.manager.ViewManager;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ClusterService;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.BooleanUtils;
import org.controlsfx.control.SegmentedButton;

import java.util.Optional;
import java.util.function.Consumer;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.buttonToggleGroupListener;
import static com.prafka.desktop.util.JavaFXUtils.getStage;

public class AddEditClusterController extends AbstractController {

    public SegmentedButton segmentedButtonTab;
    public ToggleButton tabKafkaCluster;
    public ToggleButton tabSchemaRegistry;
    public ToggleButton tabKafkaConnect;
    public Pane paneTabContent;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonSave;

    private final KafkaManager kafkaManager;
    private final SerDeManager serDeManager;
    private final ClusterService clusterService;
    private ViewManager.View<AddEditClusterTabKafkaClusterController> tabKafkaClusterView;
    private ViewManager.View<AddEditClusterTabSchemaRegistryController> tabSchemaRegistryView;
    private ViewManager.View<AddEditClusterTabKafkaConnectController> tabKafkaConnectView;
    private Optional<ClusterModel> cluster = Optional.empty();
    private int initialTabIndex = 0;
    private Runnable onSuccess;

    @Inject
    public AddEditClusterController(KafkaManager kafkaManager, SerDeManager serDeManager, ClusterService clusterService) {
        this.kafkaManager = kafkaManager;
        this.serDeManager = serDeManager;
        this.clusterService = clusterService;
    }

    public void setData(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setData(ClusterModel cluster, int initialTabIndex, Runnable onSuccess) {
        this.cluster = Optional.of(cluster);
        this.initialTabIndex = initialTabIndex;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        tabKafkaCluster.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneTabContent.getChildren().setAll(tabKafkaClusterView.root());
            paneAlert.getChildren().clear();
        });
        tabSchemaRegistry.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneTabContent.getChildren().setAll(tabSchemaRegistryView.root());
            paneAlert.getChildren().clear();
        });
        tabKafkaConnect.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneTabContent.getChildren().setAll(tabKafkaConnectView.root());
            paneAlert.getChildren().clear();
        });

        segmentedButtonTab.getToggleGroup().selectedToggleProperty().addListener(buttonToggleGroupListener());

        buttonCancel.setOnAction(it -> getStage(it).close());

        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            try {
                tabKafkaClusterView.controller().validate();
            } catch (IllegalArgumentException e) {
                tabKafkaCluster.setSelected(true);
                sceneService.addLabelError(paneAlert, e.getMessage());
                return;
            }

            if (tabSchemaRegistryView.controller().isDefined()) {
                try {
                    tabSchemaRegistryView.controller().validate();
                } catch (IllegalArgumentException e) {
                    tabSchemaRegistry.setSelected(true);
                    sceneService.addLabelError(paneAlert, e.getMessage());
                    return;
                }
            }

            if (tabKafkaConnectView.controller().isDefined()) {
                try {
                    tabKafkaConnectView.controller().validate();
                } catch (IllegalArgumentException e) {
                    tabKafkaConnect.setSelected(true);
                    sceneService.addLabelError(paneAlert, e.getMessage());
                    return;
                }
            }

            progressIndicator.setVisible(true);
            buttonCancel.setDisable(true);
            buttonSave.setDisable(true);

            Runnable onSuccessTestConnection = () -> {
                var cluster = this.cluster.orElseGet(() -> {
                    var c = new ClusterModel();
                    c.setCurrent(clusterService.getClusters().isEmpty());
                    return c;
                });
                tabKafkaClusterView.controller().fillClusterModel(cluster);
                tabSchemaRegistryView.controller().fillClusterModel(cluster);
                tabKafkaConnectView.controller().fillClusterModel(cluster);
                clusterService.saveCluster(cluster);
                task(() -> {
                    kafkaManager.close(cluster.getId());
                    serDeManager.close(cluster.getId());
                })
                        .onSuccess(it -> {
                            getStage(actionEvent).close();
                            onSuccess.run();
                        })
                        .onError(it -> {
                            getStage(actionEvent).close();
                            onSuccess.run();
                        })
                        .start();
            };

            Consumer<Throwable> onErrorTestConnection = throwable -> {
                progressIndicator.setVisible(false);
                buttonCancel.setDisable(false);
                buttonSave.setDisable(false);
                sceneService.addLabelError(paneAlert, i18nService.get("createClusterView.labelFailedTestConnection"));
                sceneService.addHyperlinkErrorDetailed(paneAlert, throwable);
                logError(throwable);
            };

            task(tabKafkaClusterView.controller().getTaskForTestConnection())
                    .onSuccess(clusterCheck -> {
                        if (tabSchemaRegistryView.controller().isDefined()) {
                            task(tabSchemaRegistryView.controller().getTaskForTestConnection())
                                    .onSuccess(schemaCheck -> onSuccessTestConnection.run())
                                    .onError(throwable -> {
                                        tabSchemaRegistry.setSelected(true);
                                        onErrorTestConnection.accept(throwable);
                                    })
                                    .start();
                        } else {
                            onSuccessTestConnection.run();
                        }
                    })
                    .onError(throwable -> {
                        tabKafkaCluster.setSelected(true);
                        onErrorTestConnection.accept(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        tabKafkaClusterView = viewManager.loadAddEditClusterTabKafkaClusterView(this, cluster);
        tabSchemaRegistryView = viewManager.loadAddEditClusterTabSchemaRegistryView(this, cluster);
        tabKafkaConnectView = viewManager.loadAddEditClusterTabKafkaConnectView(this, cluster);

        if (initialTabIndex == 0) tabKafkaCluster.setSelected(true);
        if (initialTabIndex == 1) tabSchemaRegistry.setSelected(true);
        if (initialTabIndex == 2) tabKafkaConnect.setSelected(true);
    }

    @Override
    protected void onEnter() {
        buttonSave.fire();
    }
}
