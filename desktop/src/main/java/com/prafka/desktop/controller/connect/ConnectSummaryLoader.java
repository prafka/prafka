package com.prafka.desktop.controller.connect;

import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractSummaryLoader;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loader for Kafka Connect summary cards displayed on the cluster overview dashboard.
 *
 * <p>Asynchronously fetches connector and task counts (total, running, failed)
 * to populate summary cards in the UI.
 */
@Singleton
public class ConnectSummaryLoader extends AbstractSummaryLoader {

    private final ConnectService connectService;

    @Inject
    public ConnectSummaryLoader(ConnectService connectService) {
        this.connectService = connectService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneConnectorCount = new VBox();
        var paneRunConnectorCount = new VBox();
        var paneFailConnectorCount = new VBox();
        var paneRunTaskCount = new VBox();
        var paneFailTaskCount = new VBox();

        var task = FutureServiceAdapter.futureTask(() -> connectService.getAllConnectorsSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.connectorCount()), paneConnectorCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.runConnectorCount()), paneRunConnectorCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.failConnectorCount()), paneFailConnectorCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.runTaskCount()), paneRunTaskCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.failTaskCount()), paneFailTaskCount);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneConnectorCount, paneRunConnectorCount, paneFailConnectorCount, paneRunTaskCount, paneFailTaskCount);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task);

        return List.of(
                card(i18nService.get("connect.connectors"), paneConnectorCount),
                card(i18nService.get("connect.runningConnectors"), paneRunConnectorCount),
                card(i18nService.get("connect.failedConnectors"), paneFailConnectorCount),
                card(i18nService.get("connect.runningTasks"), paneRunTaskCount),
                card(i18nService.get("connect.failedTasks"), paneFailTaskCount)
        );
    }

    public void load(Consumer<List<Node>> onSuccess) {
        FutureServiceAdapter.futureTask(() -> connectService.getAllConnectorsSummary(clusterId()))
                .onSuccess(summary -> {
                    onSuccess.accept(List.of(
                            card(i18nService.get("connect.connectors"), new VBox(JavaFXUtils.label(summary.connectorCount())), false),
                            card(i18nService.get("connect.runningConnectors"), new VBox(JavaFXUtils.label(summary.runConnectorCount())), false),
                            card(i18nService.get("connect.failedConnectors"), new VBox(JavaFXUtils.label(summary.failConnectorCount())), false),
                            card(i18nService.get("connect.runningTasks"), new VBox(JavaFXUtils.label(summary.runTaskCount())), false),
                            card(i18nService.get("connect.failedTasks"), new VBox(JavaFXUtils.label(summary.failTaskCount())), false)
                    ));
                })
                .start();
    }
}
