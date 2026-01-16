package com.prafka.desktop.controller.acl;

import com.prafka.core.service.AclService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractSummaryLoader;
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
public class AclSummaryLoader extends AbstractSummaryLoader {

    private final AclService aclService;

    @Inject
    public AclSummaryLoader(AclService aclService) {
        this.aclService = aclService;
    }

    public List<Node> load(Supplier<Stage> primaryStage, Supplier<List<FutureServiceAdapter<?>>> futureTasks) {
        var paneAclCount = new VBox();
        var panePrincipalCount = new VBox();
        var paneTopicCount = new VBox();
        var paneConsumerGroupCount = new VBox();

        var task = futureTask(() -> aclService.getAllAclSummary(clusterId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.aclCount()), paneAclCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.principalCount()), panePrincipalCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.topicCount()), paneTopicCount);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.groupCount()), paneConsumerGroupCount);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneAclCount, panePrincipalCount, paneTopicCount, paneConsumerGroupCount);
                    logError(it);
                })
                .startNow();
        futureTasks.get().add(task);

        return List.of(
                card(i18nService.get("common.acl"), paneAclCount),
                card(i18nService.get("common.principals"), panePrincipalCount),
                card(i18nService.get("common.topics"), paneTopicCount),
                card(i18nService.get("common.consumerGroups"), paneConsumerGroupCount)
        );
    }
}
