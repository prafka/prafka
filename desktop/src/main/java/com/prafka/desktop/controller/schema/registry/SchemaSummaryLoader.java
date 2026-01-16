package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractSummaryLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Supplier;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.FormatUtils.prettyEnum;
import static com.prafka.desktop.util.JavaFXUtils.*;

@Singleton
public class SchemaSummaryLoader extends AbstractSummaryLoader {

    private final SchemaRegistryService schemaRegistryService;

    @Inject
    public SchemaSummaryLoader(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneSubjectCount = new VBox();
        var paneSoftDeletedCount = new VBox();
        var paneGlobalCompatibility = new VBox();
        var paneMode = new VBox();

        var task = futureTask(() -> schemaRegistryService.getAllSchemasSummary(clusterId()))
                .onSuccess(summary -> {
                    setLabel(label(summary.count()), paneSubjectCount);
                    setLabel(label(summary.countDeleted()), paneSoftDeletedCount);
                    setLabel(label(prettyEnum(summary.compatibility())), paneGlobalCompatibility);
                    setLabel(label(prettyEnum(summary.mode())), paneMode);
                })
                .onError(it -> {
                    setPaneNA(paneSubjectCount, paneSoftDeletedCount, paneGlobalCompatibility, paneMode);
                    loadDataError(primaryStage.get(), Pos.BOTTOM_RIGHT, it);
                })
                .startNow();
        futureTasks.get().add(task);

        return List.of(
                card(i18nService.get("schemaListView.subjectCount"), paneSubjectCount),
                card(i18nService.get("common.softDeleted"), paneSoftDeletedCount),
                card(i18nService.get("common.compatibility"), paneGlobalCompatibility),
                card(i18nService.get("common.mode"), paneMode)
        );
    }
}
