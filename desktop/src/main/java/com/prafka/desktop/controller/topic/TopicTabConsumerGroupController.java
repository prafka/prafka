package com.prafka.desktop.controller.topic;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.consumer.group.ConsumerGroupHelper;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.util.JavaFXUtils.labelWithTooltip;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for the topic consumer groups tab showing groups consuming from the topic.
 *
 * <p>Displays consumer groups with their state, member count, and lag for
 * the specific topic in a searchable table view.
 */
public class TopicTabConsumerGroupController extends AbstractTableController<List<ConsumerGroup>, String, TopicTabConsumerGroupController.ConsumerGroupModelView> {

    private final ConsumerGroupService consumerGroupService;
    private final ConsumerGroupHelper consumerGroupHelper;
    private String topicName;

    @Inject
    public TopicTabConsumerGroupController(ConsumerGroupService consumerGroupService, ConsumerGroupHelper consumerGroupHelper) {
        this.consumerGroupService = consumerGroupService;
        this.consumerGroupHelper = consumerGroupHelper;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    protected void initTable() {
        var columnId = JavaFXUtils.<ConsumerGroupModelView, Label>tableColumn(i18nService.get("common.groupId"));
        columnId.setCellValueFactory(it -> it.getValue().idProperty());
        columnId.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnId.setMinWidth(200);

        var columnState = JavaFXUtils.<ConsumerGroupModelView, Label>tableColumn(i18nService.get("common.state"));
        columnState.setCellValueFactory(it -> it.getValue().stateProperty());
        columnState.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnState.setPrefWidth(200);

        var columnMembers = JavaFXUtils.<ConsumerGroupModelView, NumberLabel>tableColumn(i18nService.get("common.members"));
        columnMembers.setCellValueFactory(it -> it.getValue().membersProperty());
        columnMembers.setComparator(NumberLabel.COMPARATOR);
        columnMembers.setPrefWidth(150);

        var columnLag = JavaFXUtils.<ConsumerGroupModelView, NumberLabel>tableColumn(i18nService.get("common.lag"));
        columnLag.setCellValueFactory(it -> it.getValue().lagProperty());
        columnLag.setComparator(NumberLabel.COMPARATOR);
        columnLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnState, columnMembers, columnMembers, columnLag).multiply(0.9);
        columnId.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnId, columnState, columnMembers, columnLag);
    }

    @Override
    protected CompletionStage<List<ConsumerGroup>> getLoadTableDataFuture() {
        return consumerGroupService.getAllByTopic(clusterId(), topicName);
    }

    @Override
    protected List<Map.Entry<String, ConsumerGroupModelView>> mapLoadTableDataSource(List<ConsumerGroup> consumerGroupList) {
        return consumerGroupList.stream()
                .sorted(Comparator.comparing(ConsumerGroup::getId))
                .map(it -> Map.entry(it.getId(), new ConsumerGroupModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(ConsumerGroupModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var group = model.getSource();
        if (Strings.CI.contains(group.getId(), search)
                || Strings.CI.contains(group.getState().name(), search)) {
            return true;
        }
        return false;
    }

    public class ConsumerGroupModelView extends AbstractTableModelView {

        private final ConsumerGroup source;
        private final SimpleObjectProperty<Label> id = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> members = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> lag = new SimpleObjectProperty<>();

        public ConsumerGroupModelView(ConsumerGroup source) {
            this.source = source;
            id.set(JavaFXUtils.labelWithTooltip(source.getId(), "font-medium"));
            state.set(labelWithTooltip(
                    source.getState().name(),
                    JavaFXUtils.tooltip(consumerGroupHelper.getStateDescription(source.getState())),
                    "badge", consumerGroupHelper.getStateStyle(source.getState())
            ));
            members.set(JavaFXUtils.numberLabel(source.getMembers().size(), "font-code"));
            lag.set(JavaFXUtils.numberLabel(source.getTopicLag(topicName), "font-code"));
        }

        public ConsumerGroup getSource() {
            return source;
        }

        public SimpleObjectProperty<Label> idProperty() {
            return id;
        }

        public SimpleObjectProperty<Label> stateProperty() {
            return state;
        }

        public SimpleObjectProperty<NumberLabel> membersProperty() {
            return members;
        }

        public SimpleObjectProperty<NumberLabel> lagProperty() {
            return lag;
        }
    }
}
