package com.prafka.desktop.controller.topic;

import com.prafka.core.service.LogDirService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractSummaryLoader;
import com.prafka.desktop.util.FormatUtils;
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
public class TopicSummaryLoader extends AbstractSummaryLoader {

    private final TopicService topicService;
    private final LogDirService logDirService;

    @Inject
    public TopicSummaryLoader(TopicService topicService, LogDirService logDirService) {
        this.topicService = topicService;
        this.logDirService = logDirService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneTopicCount = new VBox();
        var panePartitionCount = new VBox();
        var paneRecordCount = new VBox();
        var paneSize = new VBox();

        var task1 = futureTask(() -> topicService.getAllTopicsSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.topicCount()), paneTopicCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.partitionCount()), panePartitionCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.recordCount()), paneRecordCount);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneTopicCount, panePartitionCount, paneRecordCount);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task1);

        var task2 = futureTask(() -> logDirService.getAllTopicsSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(FormatUtils.prettySizeInBytes(summary.size())), paneSize);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneSize);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task2);

        return List.of(
                card(i18nService.get("common.topics"), paneTopicCount),
                card(i18nService.get("common.partitions"), panePartitionCount),
                card(i18nService.get("common.records"), paneRecordCount),
                card(i18nService.get("common.size"), paneSize)
        );
    }
}
