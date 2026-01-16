package com.prafka.desktop.controller.broker;

import com.prafka.core.service.BrokerService;
import com.prafka.core.service.ConfigService;
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
import java.util.function.Supplier;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

@Singleton
public class BrokerSummaryLoader extends AbstractSummaryLoader {

    private final BrokerService brokerService;
    private final ConfigService configService;

    @Inject
    public BrokerSummaryLoader(BrokerService brokerService, ConfigService configService) {
        this.brokerService = brokerService;
        this.configService = configService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneBrokerCount = new VBox();
        var paneController = new VBox();
        var paneVersion = new VBox();

        var task1 = futureTask(() -> brokerService.getAllBrokersSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.brokerCount()), paneBrokerCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.controller() ? i18nService.get("common.yes") : i18nService.get("common.no")), paneController);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneBrokerCount, paneController);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task1);

        var task2 = futureTask(() -> configService.getAllBrokersSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.version()), paneVersion);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneVersion);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task2);

        return List.of(
                card(i18nService.get("common.brokers"), paneBrokerCount),
                card(i18nService.get("common.controller"), paneController),
                card(i18nService.get("common.version"), paneVersion)
        );
    }
}
