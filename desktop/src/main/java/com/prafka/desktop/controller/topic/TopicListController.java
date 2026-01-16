package com.prafka.desktop.controller.topic;

import com.prafka.core.model.LogDir;
import com.prafka.core.model.Topic;
import com.prafka.core.service.LogDirService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static com.prafka.desktop.util.JavaFXUtils.setNumberLabelNA;

public class TopicListController extends AbstractTableController<Set<String>, String, TopicListController.TopicModelView> {

    public Button buttonEmptyTopics;
    public Button buttonDeleteTopics;
    public Button buttonCreateTopic;
    public Pane paneSummaryBlock;
    public CheckBox checkBoxShowInternalTopics;
    public CheckBox checkBoxShowStreamTopics;

    private final TopicSummaryLoader topicSummaryLoader;
    private final TopicService topicService;
    private final LogDirService logDirService;
    private final List<String> internalRegexList = List.of(
            "^_.*$", "^.*_schemas$",
            "^.*connect-configs$", "^.*connect-offsets$", "^.*connect-statuses$",
            "^.*connect-config$", "^.*connect-offset$", "^.*connect-status$"
    );
    private final List<String> streamRegexList = List.of("^.*-changelog$", "^.*-repartition$", "^.*-rekey$");

    @Inject
    public TopicListController(TopicSummaryLoader topicSummaryLoader, TopicService topicService, LogDirService logDirService) {
        this.topicSummaryLoader = topicSummaryLoader;
        this.topicService = topicService;
        this.logDirService = logDirService;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        buttonEmptyTopics.visibleProperty().bind(anyCheckBoxSelected);
        buttonEmptyTopics.setOnAction(actionEvent -> {
            var topicNameList = tableView.getItems().stream().filter(it -> it.getCheckBox().isSelected()).map(TopicModelView::getName).toList();
            viewManager.showEmptyTopicsConfirmView(JavaFXUtils.getStage(actionEvent), topicNameList, confirmCallback ->
                    futureTask(() -> topicService.empty(clusterId(), topicNameList))
                            .onSuccess(it -> {
                                confirmCallback.onSuccess();
                                sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicsEmptied"));
                                loadData();
                            })
                            .onError(confirmCallback::onError)
                            .start()
            );
        });

        buttonDeleteTopics.visibleProperty().bind(anyCheckBoxSelected);
        buttonDeleteTopics.setOnAction(actionEvent -> {
            var topicNameList = tableView.getItems().stream().filter(it -> it.getCheckBox().isSelected()).map(TopicModelView::getName).toList();
            viewManager.showDeleteTopicsConfirmView(JavaFXUtils.getStage(actionEvent), topicNameList, confirmCallback ->
                    futureTask(() -> topicService.delete(clusterId(), topicNameList))
                            .onSuccess(it -> {
                                confirmCallback.onSuccess();
                                sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicsDeleted"));
                                loadData();
                            })
                            .onError(confirmCallback::onError)
                            .start()
            );
        });

        buttonCreateTopic.setOnAction(it -> {
            var stage = JavaFXUtils.getStage(it);
            viewManager.showCreateTopicView(stage, () -> {
                sceneService.showSnackbarSuccess(stage, Pos.BOTTOM_RIGHT, i18nService.get("topic.topicCreated"));
                loadData();
            });
        });

        checkBoxShowInternalTopics.setOnAction(it -> filterTableData());
        checkBoxShowStreamTopics.setOnAction(it -> filterTableData());
    }

    @Override
    protected void initTable() {
        var columnName = JavaFXUtils.<TopicModelView, Label>tableColumn(i18nService.get("common.name"));
        columnName.setCellValueFactory(it -> it.getValue().nameProperty());
        columnName.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnName.setMinWidth(200);

        var columnPartitions = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.partitions"));
        columnPartitions.setCellValueFactory(it -> it.getValue().partitionsProperty());
        columnPartitions.setComparator(NumberLabel.COMPARATOR);
        columnPartitions.setPrefWidth(120);

        var columnReplicas = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.replicas"));
        columnReplicas.setCellValueFactory(it -> it.getValue().replicasProperty());
        columnReplicas.setComparator(NumberLabel.COMPARATOR);
        columnReplicas.setPrefWidth(120);

        var columnCount = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.records"));
        columnCount.setCellValueFactory(it -> it.getValue().countProperty());
        columnCount.setComparator(NumberLabel.COMPARATOR);
        columnCount.setPrefWidth(120);

        var columnSize = JavaFXUtils.<TopicModelView, NumberLabel>tableColumn(i18nService.get("common.size"));
        columnSize.setCellValueFactory(it -> it.getValue().sizeProperty());
        columnSize.setComparator(NumberLabel.COMPARATOR);
        columnSize.setPrefWidth(120);

        var columnCheckBox = JavaFXUtils.<TopicModelView, Node>tableColumn();
        columnCheckBox.setGraphic(checkBoxHeader);
        columnCheckBox.setCellValueFactory(it -> it.getValue().checkBoxProperty());
        columnCheckBox.setSortable(false);
        columnCheckBox.setPrefWidth(60);

        var columnActions = JavaFXUtils.<TopicModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPartitions, columnReplicas, columnCount, columnSize, columnCheckBox, columnActions).multiply(0.9);
        columnName.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnName, columnPartitions, columnReplicas, columnCount, columnSize, columnCheckBox, columnActions);

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                eventService.fire(new EventService.DashboardContentEvent(viewManager.loadTopicView(newValue.getName())));
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(topicSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<Set<String>> getLoadTableDataFuture() {
        return topicService.getAllNames(clusterId());
    }

    @Override
    protected List<Map.Entry<String, TopicModelView>> mapLoadTableDataSource(Set<String> topicNameList) {
        return topicNameList.stream().sorted().map(it -> Map.entry(it, new TopicModelView(it))).toList();
    }

    @Override
    protected void loadTableFullData() {
        var task1 = futureTask(() -> topicService.getAll(clusterId(), modelMap.keySet()))
                .onSuccess(topicMap -> {
                    modelMap.forEach((topicName, model) -> {
                        var topic = topicMap.get(topicName);
                        if (topic != null) {
                            model.setPartitions(topic);
                            model.setReplicas(topic);
                            model.setCount(topic);
                        } else {
                            JavaFXUtils.setNumberLabelNA(model.partitionsProperty(), model.replicasProperty(), model.countProperty());
                        }
                    });
                })
                .onError(it -> {
                    modelMap.values().forEach(model -> JavaFXUtils.setNumberLabelNA(model.partitionsProperty(), model.replicasProperty(), model.countProperty()));
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task1);

        var task2 = futureTask(() -> logDirService.getAllByTopics(clusterId(), modelMap.keySet()))
                .onSuccess(logDirMap -> {
                    modelMap.forEach((topicName, model) -> {
                        var logDir = logDirMap.get(topicName);
                        if (logDir != null) {
                            model.setSize(logDir);
                        } else {
                            setNumberLabelNA(model.countProperty());
                        }
                    });
                })
                .onError(it -> {
                    modelMap.values().forEach(model -> setNumberLabelNA(model.countProperty()));
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task2);
    }

    @Override
    protected boolean getFilterTableDataPredicate(TopicModelView model) {
        // todo make better
        var condition = true;
        var topicName = model.getName().toLowerCase();
        var search = textFieldSearch.getText();
        var showInternal = checkBoxShowInternalTopics.isSelected();
        var showStream = checkBoxShowStreamTopics.isSelected();
        if (StringUtils.isNotBlank(search) && !topicName.contains(search.toLowerCase())) {
            condition = false;
        }
        if (!showInternal && internalRegexList.stream().anyMatch(topicName::matches)) {
            condition = false;
        }
        if (!showStream && streamRegexList.stream().anyMatch(topicName::matches)) {
            condition = false;
        }
        return condition;
    }

    public class TopicModelView extends AbstractTableModelView {

        private final SimpleObjectProperty<Label> name = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partitions = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> replicas = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> count = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> size = new SimpleObjectProperty<>();

        public TopicModelView(String topicName) {
            name.set(JavaFXUtils.labelWithTooltip(topicName, "font-medium"));
            JavaFXUtils.setNumberLabelLoader(themeService.getIconLoader16(), partitions, replicas, count, size);
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemEmptyTopic = new MenuItem(i18nService.get("topic.emptyTopic"));
            menuItemEmptyTopic.setOnAction(actionEvent ->
                    viewManager.showEmptyTopicConfirmView(JavaFXUtils.getStage(actionEvent), getName(), confirmCallback ->
                            futureTask(() -> topicService.empty(clusterId(), getName()))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicEmptied"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );

            var menuItemDeleteTopic = new MenuItem(i18nService.get("topic.deleteTopic"));
            menuItemDeleteTopic.setOnAction(actionEvent ->
                    viewManager.showDeleteTopicConfirmView(JavaFXUtils.getStage(actionEvent), getName(), confirmCallback ->
                            futureTask(() -> topicService.delete(clusterId(), getName()))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicDeleted"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );

            // todo add support for add partitions

            actions.set(sceneService.createCellActionsMenuButton(menuItemEmptyTopic, menuItemDeleteTopic));
        }

        public String getName() {
            return name.get().getText();
        }

        public SimpleObjectProperty<Label> nameProperty() {
            return name;
        }

        public SimpleObjectProperty<NumberLabel> partitionsProperty() {
            return partitions;
        }

        public void setPartitions(Topic topic) {
            partitions.set(JavaFXUtils.numberLabel(topic.getPartitions().size(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> replicasProperty() {
            return replicas;
        }

        public void setReplicas(Topic topic) {
            replicas.set(JavaFXUtils.numberLabel(topic.getReplicaCount(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> countProperty() {
            return count;
        }

        public void setCount(Topic topic) {
            count.set(JavaFXUtils.numberLabel(topic.getRecordCount(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> sizeProperty() {
            return size;
        }

        public void setSize(Map<TopicPartition, List<LogDir>> logDirs) {
            var sizeInBytes = logDirs.values().stream().flatMap(Collection::stream).map(LogDir::getSize).reduce(0L, Long::sum);
            size.set(JavaFXUtils.numberLabelText(sizeInBytes, FormatUtils.prettySizeInBytes(sizeInBytes), "font-code"));
        }
    }
}
