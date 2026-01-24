package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.service.ConsumerGroupService;
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

/**
 * Loader for consumer group summary cards displayed on the cluster overview dashboard.
 *
 * <p>Asynchronously fetches group counts by state (total, stable, rebalancing, empty, dead)
 * to populate summary cards in the UI.
 */
@Singleton
public class ConsumerGroupSummaryLoader extends AbstractSummaryLoader {

    private final ConsumerGroupService consumerGroupService;

    @Inject
    public ConsumerGroupSummaryLoader(ConsumerGroupService consumerGroupService) {
        this.consumerGroupService = consumerGroupService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneGroupCount = new VBox();
        var paneStableCount = new VBox();
        var paneRebalanceCount = new VBox();
        var paneEmptyCount = new VBox();
        var paneDeadCount = new VBox();

        var task = futureTask(() -> consumerGroupService.getAllGroupsSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.groupCount()), paneGroupCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.stableCount()), paneStableCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.preparingRebalanceCount() + summary.completingRebalanceCount()), paneRebalanceCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.emptyCount()), paneEmptyCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.deadCount()), paneDeadCount);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneGroupCount, paneStableCount, paneRebalanceCount, paneEmptyCount, paneDeadCount);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task);

        return List.of(
                card(i18nService.get("common.groups"), paneGroupCount),
                card(i18nService.get("common.stable"), paneStableCount),
                card(i18nService.get("common.rebalance"), paneRebalanceCount),
                card(i18nService.get("common.empty"), paneEmptyCount),
                card(i18nService.get("common.dead"), paneDeadCount)
        );
    }
}
