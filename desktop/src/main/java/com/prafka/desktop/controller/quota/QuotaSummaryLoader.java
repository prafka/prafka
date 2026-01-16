package com.prafka.desktop.controller.quota;

import com.prafka.core.model.Quota;
import com.prafka.core.service.QuotaService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractSummaryLoader;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Supplier;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

@Singleton
public class QuotaSummaryLoader extends AbstractSummaryLoader {

    private final QuotaService quotaService;

    @Inject
    public QuotaSummaryLoader(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneQuotaCount = new VBox();
        var paneProducerRateCount = new VBox();
        var paneConsumerRateCount = new VBox();
        var paneConnectionRateCount = new VBox();
        var paneControllerRateCount = new VBox();
        var paneRequestPercentCount = new VBox();

        var task = futureTask(() -> quotaService.getAllQuotasSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.quotaCount()), paneQuotaCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.producerRateCount()), paneProducerRateCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.consumerRateCount()), paneConsumerRateCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.connectionRateCount()), paneConnectionRateCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.controllerRateCount()), paneControllerRateCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.requestPercentCount()), paneRequestPercentCount);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneQuotaCount, paneProducerRateCount, paneConsumerRateCount, paneConnectionRateCount, paneControllerRateCount, paneRequestPercentCount);
                    logError(it);
                })
                .startNow();
        futureTasks.get().add(task);

        return List.of(
                card(i18nService.get("common.quotas"), paneQuotaCount),
                card(FormatUtils.prettyEnum(Quota.ConfigType.PRODUCER_RATE), paneProducerRateCount),
                card(FormatUtils.prettyEnum(Quota.ConfigType.CONSUMER_RATE), paneConsumerRateCount),
                card(FormatUtils.prettyEnum(Quota.ConfigType.CONNECTION_RATE), paneConnectionRateCount),
                card(FormatUtils.prettyEnum(Quota.ConfigType.CONTROLLER_RATE), paneControllerRateCount),
                card(FormatUtils.prettyEnum(Quota.ConfigType.REQUEST_PERCENTAGE), paneRequestPercentCount)
        );
    }
}
