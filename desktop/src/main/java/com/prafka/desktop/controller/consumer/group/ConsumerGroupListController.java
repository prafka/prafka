package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static com.prafka.desktop.util.JavaFXUtils.labelWithTooltip;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for displaying the list of consumer groups in a searchable table.
 *
 * <p>Shows group ID, state, member count, topic count, and overall lag with
 * summary cards for state distribution. Supports group creation, offset reset,
 * and group deletion actions.
 */
public class ConsumerGroupListController extends AbstractTableController<List<ConsumerGroup.GroupIdState>, String, ConsumerGroupListController.ConsumerGroupModelView> {

    public Button buttonCreateConsumerGroup;
    public Pane paneSummaryBlock;

    private final ConsumerGroupSummaryLoader consumerGroupSummaryLoader;
    private final ConsumerGroupService consumerGroupService;
    private final ConsumerGroupHelper consumerGroupHelper;

    @Inject
    public ConsumerGroupListController(ConsumerGroupSummaryLoader consumerGroupSummaryLoader, ConsumerGroupService consumerGroupService, ConsumerGroupHelper consumerGroupHelper) {
        this.consumerGroupSummaryLoader = consumerGroupSummaryLoader;
        this.consumerGroupService = consumerGroupService;
        this.consumerGroupHelper = consumerGroupHelper;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        buttonCreateConsumerGroup.setOnAction(it -> {
            var stage = JavaFXUtils.getStage(it);
            viewManager.showCreateConsumerGroupView(stage, () -> {
                sceneService.showSnackbarSuccess(stage, Pos.BOTTOM_RIGHT, i18nService.get("consumerGroup.groupCreated"));
                loadData();
            });
        });
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

        var columnTopics = JavaFXUtils.<ConsumerGroupModelView, NumberLabel>tableColumn(i18nService.get("common.topics"));
        columnTopics.setCellValueFactory(it -> it.getValue().topicsProperty());
        columnMembers.setComparator(NumberLabel.COMPARATOR);
        columnTopics.setPrefWidth(120);

        var columnOverallLag = JavaFXUtils.<ConsumerGroupModelView, NumberLabel>tableColumn(i18nService.get("common.overallLag"));
        columnOverallLag.setCellValueFactory(it -> it.getValue().overallLagProperty());
        columnOverallLag.setComparator(NumberLabel.COMPARATOR);
        columnOverallLag.setPrefWidth(150);

        var columnActions = JavaFXUtils.<ConsumerGroupModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnState, columnMembers, columnTopics, columnOverallLag, columnActions).multiply(0.9);
        columnId.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnId, columnState, columnMembers, columnTopics, columnOverallLag, columnActions);

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                eventService.fire(new EventService.DashboardContentEvent(viewManager.loadConsumerGroupView(newValue.getGroupIdState())));
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(consumerGroupSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<List<ConsumerGroup.GroupIdState>> getLoadTableDataFuture() {
        return consumerGroupService.getAllGroupIdsWithState(clusterId());
    }

    @Override
    protected List<Map.Entry<String, ConsumerGroupModelView>> mapLoadTableDataSource(List<ConsumerGroup.GroupIdState> groupIdStateList) {
        return groupIdStateList.stream()
                .sorted(Comparator.comparing(ConsumerGroup.GroupIdState::groupId))
                .map(it -> Map.entry(it.groupId(), new ConsumerGroupModelView(it)))
                .toList();
    }

    @Override
    protected void loadTableFullData() {
        var task = futureTask(() -> consumerGroupService.getAll(clusterId(), modelMap.keySet().stream().toList()))
                .onSuccess(groupMap -> {
                    modelMap.forEach((groupId, model) -> {
                        var group = groupMap.get(groupId);
                        if (group != null) {
                            model.setSource(group);
                            model.setMembers(group);
                            model.setTopics(group);
                            model.setOverallLag(group);
                        } else {
                            JavaFXUtils.setNumberLabelNA(model.membersProperty(), model.topicsProperty(), model.overallLagProperty());
                        }
                    });
                })
                .onError(it -> {
                    modelMap.values().forEach(model -> {
                        JavaFXUtils.setNumberLabelNA(model.membersProperty(), model.topicsProperty(), model.overallLagProperty());
                    });
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task);
    }

    @Override
    protected boolean getFilterTableDataPredicate(ConsumerGroupModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        if (Strings.CI.contains(model.getId(), search)) {
            return true;
        }
        var group = model.getSource();
        if (group.isPresent()) {
            if (Strings.CI.contains(group.get().getState().name(), search)) {
                return true;
            }
            if (group.get().getTopics().stream().anyMatch(topic -> Strings.CI.contains(topic, search))) {
                return true;
            }
        }
        return false;
    }

    public class ConsumerGroupModelView extends AbstractTableModelView {

        private final ConsumerGroup.GroupIdState groupIdState;
        private Optional<ConsumerGroup> source = Optional.empty();
        private final SimpleObjectProperty<Label> id = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> members = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> topics = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> overallLag = new SimpleObjectProperty<>();

        public ConsumerGroupModelView(ConsumerGroup.GroupIdState groupIdState) {
            this.groupIdState = groupIdState;
            id.set(JavaFXUtils.labelWithTooltip(groupIdState.groupId(), "font-medium"));
            state.set(labelWithTooltip(
                    groupIdState.state().name(),
                    JavaFXUtils.tooltip(consumerGroupHelper.getStateDescription(groupIdState.state())),
                    "badge", consumerGroupHelper.getStateStyle(groupIdState.state())
            ));
            JavaFXUtils.setNumberLabelLoader(themeService.getIconLoader16(), members, topics, overallLag);
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemResetOffsets = new MenuItem(i18nService.get("consumerGroup.resetOffsets"));
            menuItemResetOffsets.setOnAction(sourceActionEvent ->
                    viewManager.showEditConsumerGroupView(JavaFXUtils.getStage(sourceActionEvent), getId(), () -> {
                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("consumerGroup.offsetsReset"));
                        loadData();
                    })
            );

            var menuItemDeleteGroup = new MenuItem(i18nService.get("consumerGroup.deleteGroup"));
            menuItemDeleteGroup.setOnAction(sourceActionEvent ->
                    viewManager.showDeleteConsumerGroupConfirmView(JavaFXUtils.getStage(sourceActionEvent), confirmCallback ->
                            futureTask(() -> consumerGroupService.delete(clusterId(), getId()))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("consumerGroup.groupDeleted"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );

            if (!consumerGroupHelper.isEditable(groupIdState.state())) {
                menuItemResetOffsets.setDisable(true);
                menuItemDeleteGroup.setDisable(true);
            }

            actions.set(sceneService.createCellActionsMenuButton(menuItemResetOffsets, menuItemDeleteGroup));
        }

        public ConsumerGroup.GroupIdState getGroupIdState() {
            return groupIdState;
        }

        public Optional<ConsumerGroup> getSource() {
            return source;
        }

        public void setSource(ConsumerGroup source) {
            this.source = Optional.of(source);
        }

        public String getId() {
            return groupIdState.groupId();
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

        public void setMembers(ConsumerGroup group) {
            members.set(JavaFXUtils.numberLabel(group.getMembers().size(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> topicsProperty() {
            return topics;
        }

        public void setTopics(ConsumerGroup group) {
            var label = JavaFXUtils.numberLabel(group.getTopics().size(), "font-code");
            if (CollectionUtils.isNotEmpty(group.getTopics())) {
                var items = group.getTopics().stream()
                        .map(topic -> topic + " -> " + i18nService.get("common.lag").toLowerCase() + " " + group.getTopicLag(topic))
                        .collect(Collectors.joining("\n"));
                label.setTooltip(JavaFXUtils.tooltip(items));
                label.setContentDisplay(ContentDisplay.RIGHT);
                themeService.setIcon(label, themeService.getIcon16("information_circle.png"));
            }
            topics.set(label);
        }

        public SimpleObjectProperty<NumberLabel> overallLagProperty() {
            return overallLag;
        }

        public void setOverallLag(ConsumerGroup group) {
            overallLag.set(JavaFXUtils.numberLabel(group.getOverallLag(), "font-code"));
        }
    }
}
