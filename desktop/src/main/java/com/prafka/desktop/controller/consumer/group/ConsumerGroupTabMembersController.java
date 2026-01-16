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

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ConsumerGroupTabMembersController extends AbstractTableController<ConsumerGroup, String, ConsumerGroupTabMembersController.MemberModelView> {

    private final ConsumerGroupService consumerGroupService;
    private String groupId;

    @Inject
    public ConsumerGroupTabMembersController(ConsumerGroupService consumerGroupService) {
        this.consumerGroupService = consumerGroupService;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    protected void initTable() {
        var columnExpander = new TableRowExpanderColumn<>(this::createTopicTableView);
        columnExpander.setCellFactory(JavaFXUtils.toggleCellFactory(columnExpander, themeService));
        columnExpander.setPrefWidth(35);
        columnExpander.setSortable(false);

        var columnName = JavaFXUtils.<MemberModelView, Node>tableColumn(i18nService.get("common.name"));
        columnName.setCellValueFactory(it -> it.getValue().nameProperty());
        columnName.setComparator(JavaFXUtils.BORDER_PANE_LEFT_LABEL_COMPARATOR);
        columnName.setMinWidth(200);

        var columnHost = JavaFXUtils.<MemberModelView, Label>tableColumn(i18nService.get("common.host"));
        columnHost.setCellValueFactory(it -> it.getValue().hostProperty());
        columnHost.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnHost.setMinWidth(200);

        var columnPartitions = JavaFXUtils.<MemberModelView, NumberLabel>tableColumn(i18nService.get("common.partitions"));
        columnPartitions.setCellValueFactory(it -> it.getValue().partitionsProperty());
        columnPartitions.setComparator(NumberLabel.COMPARATOR);
        columnPartitions.setPrefWidth(150);

        var columnLag = JavaFXUtils.<MemberModelView, NumberLabel>tableColumn(i18nService.get("common.lag"));
        columnLag.setCellValueFactory(it -> it.getValue().lagProperty());
        columnLag.setComparator(NumberLabel.COMPARATOR);
        columnLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnExpander, columnPartitions, columnLag).multiply(0.9);
        columnName.prefWidthProperty().bind(remainTableWidth.multiply(0.6));
        columnHost.prefWidthProperty().bind(remainTableWidth.multiply(0.4));

        //noinspection unchecked
        tableView.getColumns().addAll(columnExpander, columnName, columnHost, columnPartitions, columnLag);
        tableView.setRowFactory(JavaFXUtils.toggleRowFactory(columnExpander));
        JavaFXUtils.disableTableViewFocus(tableView);
    }

    private VBox createTopicTableView(TableRowExpanderColumn.TableRowDataFeatures<MemberModelView> param) {
        var member = param.getValue();
        if (CollectionUtils.isEmpty(member.getTopicModels())) return new VBox();

        var tableView = new TableView<TopicModelView>();
        JavaFXUtils.setTableViewAutoHeight(tableView, 10);
        JavaFXUtils.disableTableViewFocus(tableView);

        var columnTopic = JavaFXUtils.<TopicModelView, Label>tableColumn(i18nService.get("common.topic"));
        columnTopic.setCellValueFactory(it -> it.getValue().nameProperty());
        columnTopic.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnTopic.setMinWidth(200);

        var columnPartition = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnPartition.setPrefWidth(150);

        var columnOffset = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.offset"));
        columnOffset.setCellValueFactory(it -> it.getValue().offsetProperty());
        columnOffset.setComparator(NumberLabel.COMPARATOR);
        columnOffset.setPrefWidth(120);

        var columnLag = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.lag"));
        columnLag.setCellValueFactory(it -> it.getValue().lagProperty());
        columnLag.setComparator(NumberLabel.COMPARATOR);
        columnLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPartition, columnOffset, columnLag).multiply(0.9);
        columnTopic.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnTopic, columnPartition, columnOffset, columnLag);

        tableView.getItems().setAll(member.getTopicModels());

        var box = new VBox(tableView);
        VBox.setMargin(tableView, new Insets(0, 10, 0, 10));

        return box;
    }

    @Override
    protected CompletionStage<ConsumerGroup> getLoadTableDataFuture() {
        return consumerGroupService.get(clusterId(), groupId);
    }

    @Override
    protected List<Map.Entry<String, MemberModelView>> mapLoadTableDataSource(ConsumerGroup consumerGroup) {
        return consumerGroup.getMembers().stream()
                .sorted(Comparator.comparing(ConsumerGroup.Member::getClientId))
                .map(it -> Map.entry(it.getConsumerId(), new MemberModelView(consumerGroup, it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(MemberModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var member = model.getSource();
        if (Strings.CI.contains(member.getConsumerId(), search)
                || Strings.CI.contains(member.getClientId(), search)
                || Strings.CI.contains(member.getHost(), search)) {
            return true;
        }
        return false;
    }

    public static class MemberModelView extends AbstractTableModelView {

        private final ConsumerGroup.Member source;
        private final SimpleObjectProperty<Node> name = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> host = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partitions = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> lag = new SimpleObjectProperty<>();
        private final List<TopicModelView> topicModels;

        public MemberModelView(ConsumerGroup group, ConsumerGroup.Member member) {
            this.source = member;
            var pane = new BorderPane();
            pane.setLeft(JavaFXUtils.labelWithTooltip(member.getClientId(), "font-medium", "pd-r-0_4"));
            var box = new HBox(JavaFXUtils.labelWithTooltip(member.getConsumerId(), "label-desc"));
            box.setAlignment(Pos.CENTER_LEFT);
            pane.setCenter(box);
            name.set(new VBox(pane));
            host.set(JavaFXUtils.labelWithTooltip(member.getHost()));
            partitions.set(JavaFXUtils.numberLabel(member.getPartitions().size(), "font-code"));
            lag.set(JavaFXUtils.numberLabel(member.getPartitions().stream().map(it -> group.getPartitionOffsets().get(it).getLag()).reduce(0L, Long::sum), "font-code"));
            topicModels = member.getPartitions().stream().map(it -> new TopicModelView(it, group.getPartitionOffsets().get(it))).toList();
        }

        public ConsumerGroup.Member getSource() {
            return source;
        }

        public SimpleObjectProperty<Node> nameProperty() {
            return name;
        }

        public SimpleObjectProperty<Label> hostProperty() {
            return host;
        }

        public SimpleObjectProperty<NumberLabel> partitionsProperty() {
            return partitions;
        }

        public SimpleObjectProperty<NumberLabel> lagProperty() {
            return lag;
        }

        public List<TopicModelView> getTopicModels() {
            return topicModels;
        }
    }

    public static class TopicModelView {

        private final SimpleObjectProperty<Label> name = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> offset = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> lag = new SimpleObjectProperty<>();

        public TopicModelView(TopicPartition tp, ConsumerGroup.Offset offset) {
            name.set(JavaFXUtils.labelWithTooltip(tp.topic()));
            partition.set(JavaFXUtils.numberLabel(tp.partition(), "font-code"));
            this.offset.set(JavaFXUtils.numberLabel(offset.current(), "font-code"));
            lag.set(JavaFXUtils.numberLabel(offset.getLag(), "font-code"));
        }

        public SimpleObjectProperty<Label> nameProperty() {
            return name;
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<NumberLabel> offsetProperty() {
            return offset;
        }

        public SimpleObjectProperty<NumberLabel> lagProperty() {
            return lag;
        }
    }
}
