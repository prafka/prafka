package com.prafka.desktop.controller.cluster;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.controller.acl.AclSummaryLoader;
import com.prafka.desktop.controller.broker.BrokerSummaryLoader;
import com.prafka.desktop.controller.connect.ConnectSummaryLoader;
import com.prafka.desktop.controller.consumer.group.ConsumerGroupSummaryLoader;
import com.prafka.desktop.controller.quota.QuotaSummaryLoader;
import com.prafka.desktop.controller.schema.registry.SchemaSummaryLoader;
import com.prafka.desktop.controller.topic.TopicSummaryLoader;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static com.prafka.desktop.util.JavaFXUtils.label;

public class ClusterOverviewController extends AbstractController {

    public Pane paneContent;

    private final BrokerSummaryLoader brokerSummaryLoader;
    private final TopicSummaryLoader topicSummaryLoader;
    private final ConsumerGroupSummaryLoader consumerGroupSummaryLoader;
    private final SchemaSummaryLoader schemaSummaryLoader;
    private final AclSummaryLoader aclSummaryLoader;
    private final QuotaSummaryLoader quotaSummaryLoader;
    private final ConnectSummaryLoader connectSummaryLoader;

    @Inject
    public ClusterOverviewController(BrokerSummaryLoader brokerSummaryLoader, TopicSummaryLoader topicSummaryLoader, ConsumerGroupSummaryLoader consumerGroupSummaryLoader, SchemaSummaryLoader schemaSummaryLoader, AclSummaryLoader aclSummaryLoader, QuotaSummaryLoader quotaSummaryLoader, ConnectSummaryLoader connectSummaryLoader) {
        this.brokerSummaryLoader = brokerSummaryLoader;
        this.topicSummaryLoader = topicSummaryLoader;
        this.consumerGroupSummaryLoader = consumerGroupSummaryLoader;
        this.schemaSummaryLoader = schemaSummaryLoader;
        this.aclSummaryLoader = aclSummaryLoader;
        this.quotaSummaryLoader = quotaSummaryLoader;
        this.connectSummaryLoader = connectSummaryLoader;
    }

    @Override
    public void initUi() {
        paneContent.getChildren().clear();

        paneContent.getChildren().add(paneSummary(i18nService.get("common.brokers"), brokerSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));

        paneContent.getChildren().add(new HBox() {{
            getStyleClass().add("pd-t-2");
        }});
        paneContent.getChildren().add(paneSummary(i18nService.get("common.topics"), topicSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));

        paneContent.getChildren().add(new HBox() {{
            getStyleClass().add("pd-t-2");
        }});
        paneContent.getChildren().add(paneSummary(i18nService.get("common.consumerGroups"), consumerGroupSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));

        if (sessionService.getCluster().isSchemaRegistryDefined()) {
            paneContent.getChildren().add(new HBox() {{
                getStyleClass().add("pd-t-2");
            }});
            paneContent.getChildren().add(paneSummary(i18nService.get("common.schemaRegistry"), schemaSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));
        }

        paneContent.getChildren().add(new HBox() {{
            getStyleClass().add("pd-t-2");
        }});
        paneContent.getChildren().add(paneSummary(i18nService.get("common.acl"), aclSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));

        paneContent.getChildren().add(new HBox() {{
            getStyleClass().add("pd-t-2");
        }});
        paneContent.getChildren().add(paneSummary(i18nService.get("common.quotas"), quotaSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));

        if (sessionService.getCluster().isConnectsDefined()) {
            paneContent.getChildren().add(new HBox() {{
                getStyleClass().add("pd-t-2");
            }});
            paneContent.getChildren().add(paneSummary(i18nService.get("common.kafkaConnect"), connectSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks)));
        }
    }

    private Pane paneSummary(String title, List<Node> content) {
        return new VBox(
                new HBox(label(title, "h1")) {{
                    getStyleClass().add("content-block-header");
                }},
                new FlowPane(15, 10) {{
                    getStyleClass().addAll("pd-t-1", "summary-block");
                    getChildren().setAll(content);
                }}
        );
    }
}
