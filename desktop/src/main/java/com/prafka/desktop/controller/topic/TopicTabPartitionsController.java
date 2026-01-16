package com.prafka.desktop.controller.topic;

import com.prafka.core.model.LogDir;
import com.prafka.core.model.Topic;
import com.prafka.core.service.LogDirService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.setNumberLabelLoader;
import static com.prafka.desktop.util.JavaFXUtils.setNumberLabelNA;

public class TopicTabPartitionsController extends AbstractTableController<Topic, Integer, TopicTabPartitionsController.PartitionModelView> {

    private final TopicService topicService;
    private final LogDirService logDirService;
    private String topicName;

    @Inject
    public TopicTabPartitionsController(TopicService topicService, LogDirService logDirService) {
        this.topicService = topicService;
        this.logDirService = logDirService;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    protected void initTable() {
        var columnPartition = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnPartition.setPrefWidth(140);

        var columnCount = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.records"));
        columnCount.setCellValueFactory(it -> it.getValue().countProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnCount.setPrefWidth(140);

        var columnSize = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.size"));
        columnSize.setCellValueFactory(it -> it.getValue().sizeProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnSize.setPrefWidth(140);

        var columnOffsets = JavaFXUtils.<PartitionModelView, Label>tableColumn(i18nService.get("common.offsets"));
        columnOffsets.setCellValueFactory(it -> it.getValue().offsetsProperty());
        columnOffsets.setSortable(false);
        columnOffsets.setPrefWidth(140);

        var columnReplicas = JavaFXUtils.<PartitionModelView, Node>tableColumn(i18nService.get("common.replicas"));
        columnReplicas.setCellValueFactory(it -> it.getValue().replicasProperty());
        columnReplicas.setSortable(false);
        columnReplicas.setPrefWidth(140);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView).multiply(0.9);
        var width = remainTableWidth.divide(5);
        columnPartition.prefWidthProperty().bind(width);
        columnCount.prefWidthProperty().bind(width);
        columnSize.prefWidthProperty().bind(width);
        columnOffsets.prefWidthProperty().bind(width);
        columnReplicas.prefWidthProperty().bind(width);

        //noinspection unchecked
        tableView.getColumns().addAll(columnPartition, columnCount, columnSize, columnOffsets, columnReplicas);
    }

    @Override
    protected CompletionStage<Topic> getLoadTableDataFuture() {
        return topicService.get(clusterId(), topicName);
    }

    @Override
    protected List<Map.Entry<Integer, PartitionModelView>> mapLoadTableDataSource(Topic topic) {
        return topic.getPartitions().stream()
                .sorted(Comparator.comparing(Topic.Partition::getId))
                .map(it -> Map.entry(it.getId(), new PartitionModelView(it)))
                .toList();
    }

    @Override
    protected void loadTableFullData() {
        var task = futureTask(() -> logDirService.getAllByTopic(clusterId(), topicName))
                .onSuccess(logDirMap -> {
                    modelMap.forEach((partition, model) -> {
                        var logDir = logDirMap.get(partition);
                        if (logDir != null) {
                            model.setSize(logDir);
                        } else {
                            setNumberLabelNA(model.sizeProperty());
                        }
                    });
                })
                .onError(it -> {
                    modelMap.values().forEach(model -> setNumberLabelNA(model.sizeProperty()));
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task);
    }

    @Override
    protected boolean getFilterTableDataPredicate(PartitionModelView model) {
        var search = textFieldSearch.getText();
        if (StringUtils.isBlank(search)) {
            return true;
        }
        if (Strings.CI.contains(String.valueOf(model.getPartition()), search)) {
            return true;
        }
        return false;
    }

    public class PartitionModelView extends AbstractTableModelView {

        private final Topic.Partition source;
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> count = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> size = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> offsets = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Node> replicas = new SimpleObjectProperty<>();

        public PartitionModelView(Topic.Partition source) {
            this.source = source;
            partition.set(JavaFXUtils.numberLabel(source.getId(), "font-code-medium"));
            count.set(JavaFXUtils.numberLabel(source.getEndOffset() - source.getBeginOffset(), "font-code"));
            setNumberLabelLoader(themeService.getIconLoader16(), size);
            offsets.set(JavaFXUtils.label(source.getBeginOffset() + " -> " + source.getEndOffset(), "font-code"));
            var replicasBox = new HBox(6);
            for (int i = 0; i < source.getReplicas().size(); i++) {
                var replica = source.getReplicas().get(i);
                var label = new Label(String.valueOf(replica.getId()));
                label.getStyleClass().add("badge");
                if (replica.isLeader()) {
                    label.getStyleClass().add("badge-green");
                    label.setTooltip(JavaFXUtils.tooltip(replica.getAddress() + " " + i18nService.get("common.leader").toLowerCase()));
                } else {
                    label.getStyleClass().add("badge-gray");
                    label.setTooltip(JavaFXUtils.tooltip(replica.getAddress()));
                }
                replicasBox.getChildren().add(label);
            }
            replicas.set(replicasBox);
        }

        public int getPartition() {
            return source.getId();
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<NumberLabel> countProperty() {
            return count;
        }

        public SimpleObjectProperty<NumberLabel> sizeProperty() {
            return size;
        }

        public void setSize(List<LogDir> logDirs) {
            var sizeInBytes = logDirs.stream().map(LogDir::getSize).reduce(0L, Long::sum);
            size.set(JavaFXUtils.numberLabelText(sizeInBytes, FormatUtils.prettySizeInBytes(sizeInBytes), "font-code"));
        }

        public SimpleObjectProperty<Label> offsetsProperty() {
            return offsets;
        }

        public SimpleObjectProperty<Node> replicasProperty() {
            return replicas;
        }
    }
}
