package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.TopicPartition;
import org.controlsfx.control.table.TableRowExpanderColumn;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ConsumerGroupTabTopicsController extends AbstractTableController<ConsumerGroup, String, ConsumerGroupTabTopicsController.TopicModelView> {

    private final ConsumerGroupService consumerGroupService;
    private String groupId;

    @Inject
    public ConsumerGroupTabTopicsController(ConsumerGroupService consumerGroupService) {
        this.consumerGroupService = consumerGroupService;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    protected void initTable() {
        var columnExpander = new TableRowExpanderColumn<>(this::createPartitionTableView);
        columnExpander.setCellFactory(JavaFXUtils.toggleCellFactory(columnExpander, themeService));
        columnExpander.setPrefWidth(35);
        columnExpander.setSortable(false);

        var columnTopic = JavaFXUtils.<TopicModelView, Label>tableColumn(i18nService.get("common.topic"));
        columnTopic.setCellValueFactory(it -> it.getValue().topicProperty());
        columnTopic.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnTopic.setMinWidth(200);

        var columnPartitions = JavaFXUtils.<TopicModelView, Label>tableColumn(i18nService.get("common.partitions"));
        columnPartitions.setCellValueFactory(it -> it.getValue().partitionsProperty());
        columnPartitions.setSortable(false);
        columnPartitions.setPrefWidth(150);

        var columnLag = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.lag"));
        columnLag.setCellValueFactory(it -> it.getValue().lagProperty());
        columnLag.setComparator(NumberLabel.COMPARATOR);
        columnLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnExpander, columnPartitions, columnLag).multiply(0.9);
        columnTopic.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnExpander, columnTopic, columnPartitions, columnLag);
        tableView.setRowFactory(JavaFXUtils.toggleRowFactory(columnExpander));
        JavaFXUtils.disableTableViewFocus(tableView);
    }

    private Node createPartitionTableView(TableRowExpanderColumn.TableRowDataFeatures<TopicModelView> param) {
        var member = param.getValue();
        if (CollectionUtils.isEmpty(member.getPartitionModels())) return new VBox();

        var tableView = new TableView<PartitionModelView>();
        JavaFXUtils.setTableViewAutoHeight(tableView, 10);
        JavaFXUtils.disableTableViewFocus(tableView);

        var columnPartition = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnPartition.setPrefWidth(150);

        var columnMember = JavaFXUtils.<PartitionModelView, Node>tableColumn(i18nService.get("common.member"));
        columnMember.setCellValueFactory(it -> it.getValue().memberProperty());
        columnMember.setComparator(JavaFXUtils.BORDER_PANE_LEFT_LABEL_COMPARATOR);
        columnMember.setMinWidth(200);

        var columnHost = JavaFXUtils.<PartitionModelView, Label>tableColumn(i18nService.get("common.host"));
        columnHost.setCellValueFactory(it -> it.getValue().hostProperty());
        columnHost.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnHost.setMinWidth(200);

        var columnOffset = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.offset"));
        columnOffset.setCellValueFactory(it -> it.getValue().offsetProperty());
        columnOffset.setComparator(NumberLabel.COMPARATOR);
        columnOffset.setPrefWidth(120);

        var columnLag = JavaFXUtils.<PartitionModelView, NumberLabel>tableColumn(i18nService.get("common.lag"));
        columnLag.setCellValueFactory(it -> it.getValue().lagProperty());
        columnLag.setComparator(NumberLabel.COMPARATOR);
        columnLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPartition, columnOffset, columnLag).multiply(0.9);
        columnMember.prefWidthProperty().bind(remainTableWidth.multiply(0.6));
        columnHost.prefWidthProperty().bind(remainTableWidth.multiply(0.4));

        //noinspection unchecked
        tableView.getColumns().addAll(columnPartition, columnMember, columnHost, columnOffset, columnLag);

        tableView.getItems().setAll(member.getPartitionModels());

        var box = new VBox(tableView);
        VBox.setMargin(tableView, new Insets(0, 10, 0, 10));

        return box;
    }

    @Override
    protected CompletionStage<ConsumerGroup> getLoadTableDataFuture() {
        return consumerGroupService.get(clusterId(), groupId);
    }

    @Override
    protected List<Map.Entry<String, TopicModelView>> mapLoadTableDataSource(ConsumerGroup consumerGroup) {
        return consumerGroup.getTopics().stream().sorted().map(it -> Map.entry(it, new TopicModelView(consumerGroup, it))).toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(TopicModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var topic = model.topicProperty().get().getText();
        if (Strings.CI.contains(topic, search)) {
            return true;
        }
        return false;
    }

    public class TopicModelView extends AbstractTableModelView {

        private final SimpleObjectProperty<Label> topic = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> partitions = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> lag = new SimpleObjectProperty<>();
        private final List<PartitionModelView> partitionModels;

        public TopicModelView(ConsumerGroup group, String topic) {
            this.topic.set(JavaFXUtils.labelWithTooltip(topic, "font-medium"));
            partitions.set(JavaFXUtils.label(group.getPartitions(topic).size() + " (" + group.getAssignedPartitions(topic).size() + ")", "font-code"));
            lag.set(JavaFXUtils.numberLabel(group.getTopicLag(topic), "font-code"));
            partitionModels = group.getPartitionOffsets().entrySet().stream()
                    .collect(Collectors.groupingBy(entry -> entry.getKey().topic())).entrySet().stream()
                    .filter(entry -> entry.getKey().equals(topic))
                    .flatMap(entry -> entry.getValue().stream().map(it -> new PartitionModelView(it.getKey(), it.getValue(), group.getPartitionMembers().get(it.getKey()))))
                    .sorted(Comparator.comparing(it -> it.partition.get().getSource().longValue()))
                    .toList();
        }

        public SimpleObjectProperty<Label> topicProperty() {
            return topic;
        }

        public SimpleObjectProperty<Label> partitionsProperty() {
            return partitions;
        }

        public SimpleObjectProperty<NumberLabel> lagProperty() {
            return lag;
        }

        public List<PartitionModelView> getPartitionModels() {
            return partitionModels;
        }
    }

    public class PartitionModelView {

        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Node> member = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> host = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> offset = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> lag = new SimpleObjectProperty<>();

        public PartitionModelView(TopicPartition tp, ConsumerGroup.Offset offset, ConsumerGroup.Member member) {
            partition.set(JavaFXUtils.numberLabel(tp.partition(), "font-code"));
            if (member == null) {
                var memberLabel = new Label();
                memberLabel.setTooltip(JavaFXUtils.tooltip(i18nService.get("consumerGroup.noAssignedMember")));
                themeService.setIcon16(memberLabel, "ban.png");
                this.member.set(memberLabel);
                var hostLabel = new Label();
                hostLabel.setTooltip(JavaFXUtils.tooltip(i18nService.get("consumerGroup.noAssignedMember")));
                themeService.setIcon16(hostLabel, "ban.png");
                this.host.set(hostLabel);
            } else {
                var pane = new BorderPane();
                pane.setLeft(JavaFXUtils.labelWithTooltip(member.getClientId(), "pd-r-0_4"));
                var box = new HBox(JavaFXUtils.labelWithTooltip(member.getConsumerId(), "label-desc"));
                box.setAlignment(Pos.CENTER_LEFT);
                pane.setCenter(box);
                this.member.set(pane);
                host.set(JavaFXUtils.labelWithTooltip(member.getHost()));
            }
            this.offset.set(JavaFXUtils.numberLabel(offset.current(), "font-code"));
            lag.set(JavaFXUtils.numberLabel(offset.getLag(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<Node> memberProperty() {
            return member;
        }

        public SimpleObjectProperty<Label> hostProperty() {
            return host;
        }

        public SimpleObjectProperty<NumberLabel> offsetProperty() {
            return offset;
        }

        public SimpleObjectProperty<NumberLabel> lagProperty() {
            return lag;
        }
    }
}
