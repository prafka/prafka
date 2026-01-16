package com.prafka.desktop.controller.topic;

import com.prafka.core.model.ConsumeFilter;
import com.prafka.core.model.Record;
import com.prafka.core.model.SerdeType;
import com.prafka.core.service.ConfigService;
import com.prafka.core.service.LogDirService;
import com.prafka.core.service.RecordService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.TopicFilterTemplateService;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.DateTimePicker;
import com.prafka.desktop.util.control.NumberLabel;
import com.prafka.desktop.util.control.RetentionFileChooser;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.prafka.core.util.JsonFactory.gsonPretty;
import static com.prafka.core.util.StreamUtils.tryOrEmpty;
import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.numberLabelText;
import static com.prafka.desktop.util.JavaFXUtils.setPaneLoader;

public class TopicTabConsumeController extends AbstractController {

    public Label labelCardRecordCountTitle;
    public Pane paneCardRecordCountContent;
    public Label labelCardSizeTitle;
    public Pane paneCardSizeContent;
    public Label labelCardPartitionCountTitle;
    public Pane paneCardPartitionCountContent;
    public Label labelCardReplicationFactorTitle;
    public Pane paneCardReplicationFactorContent;
    public Label labelCardCleanupPolicyTitle;
    public Pane paneCardCleanupPolicyContent;
    public HBox boxCardConsume;
    public Pane paneLoaderCardConsume;
    public Label labelCurrentCountCardConsume;
    public Label labelMaxCountCardConsume;
    public Button buttonCancelConsume;
    public Pane paneFilterBlock;
    public TextField textFieldQuickSearch;
    public ComboBox<ConsumeFilter.From.Type> comboBoxFromFilter;
    public Pane paneFromFilter;
    public TextField textFieldFromOffset;
    public DateTimePicker dateTimePickerFromDatetime;
    public TextField textFieldFromTimestamp;
    public ComboBox<String> comboBoxMaxResults;
    public ComboBox<String> comboBoxPartitions;
    public ComboBox<SerdeType> comboBoxKeyFormat;
    public ComboBox<SerdeType> comboBoxValueFormat;
    public Button buttonJsFilter;
    public Button buttonReload;
    public Button buttonTemplates;
    public Button buttonExport;
    public Pane paneJsFilters;
    public TableView<RecordModelView> tableView;
    public TableColumn<RecordModelView, NumberLabel> columnTimestamp;
    public Label labelEmptyTableView;
    public ProgressIndicator progressIndicator;

    private final TopicService topicService;
    private final LogDirService logDirService;
    private final ConfigService configService;
    private final RecordService recordService;
    private final TopicFilterTemplateService topicFilterTemplateService;
    private final List<ConsumeFilter.Expression> jsFilterList = new ArrayList<>();
    private final ObservableList<RecordModelView> modelObservableList = FXCollections.observableArrayList();
    private final FilteredList<RecordModelView> modelFilteredList = new FilteredList<>(modelObservableList, it -> true);
    private String topicName;

    @Inject
    public TopicTabConsumeController(TopicService topicService, LogDirService logDirService, ConfigService configService, RecordService recordService, TopicFilterTemplateService topicFilterTemplateService) {
        this.topicService = topicService;
        this.logDirService = logDirService;
        this.configService = configService;
        this.recordService = recordService;
        this.topicFilterTemplateService = topicFilterTemplateService;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void initFxml() {
        setPaneLoader(themeService.getIconLoader16(), paneLoaderCardConsume);
        labelMaxCountCardConsume.textProperty().bind(comboBoxMaxResults.getSelectionModel().selectedItemProperty());
        initFilters();
        initTable();
    }

    @Override
    public void initUi() {
        textFieldQuickSearch.setText(null);
        topicFilterTemplateService.getDefault(topicName).ifPresentOrElse(it -> fillFiltersOnInitUi(it.getFilter()), this::fillFiltersOnInitUi);
        modelObservableList.clear();
        tableView.getSortOrder().clear();
    }

    private void fillFiltersOnInitUi() {
        comboBoxFromFilter.getSelectionModel().select(0);
        textFieldFromOffset.setText("0");
        comboBoxMaxResults.getSelectionModel().select(3);
        comboBoxPartitions.getItems().setAll(i18nService.get("common.all"));
        comboBoxPartitions.getSelectionModel().select(0);
        futureTask(() -> topicService.get(clusterId(), topicName))
                .onSuccess(topic -> topic.getPartitions().forEach(it -> comboBoxPartitions.getItems().add(String.valueOf(it.getId()))))
                .start();
        comboBoxKeyFormat.getSelectionModel().select(0);
        comboBoxValueFormat.getSelectionModel().select(0);
        jsFilterList.clear();
        handleJsFilters();
    }

    private void fillFiltersOnInitUi(ConsumeFilter consumeFilter) {
        comboBoxFromFilter.getSelectionModel().select(consumeFilter.from().type());
        switch (consumeFilter.from().type()) {
            case OFFSET ->
                    consumeFilter.from().offset().ifPresent(it -> textFieldFromOffset.setText(String.valueOf(it)));
            case DATETIME ->
                    consumeFilter.from().timestamp().ifPresent(it -> dateTimePickerFromDatetime.setTimestampValue(it));
            case TIMESTAMP ->
                    consumeFilter.from().timestamp().ifPresent(it -> textFieldFromTimestamp.setText(String.valueOf(it)));
        }
        comboBoxMaxResults.getSelectionModel().select(String.valueOf(consumeFilter.maxResults()));
        comboBoxPartitions.getItems().setAll(i18nService.get("common.all"));
        comboBoxPartitions.getSelectionModel().select(0);
        futureTask(() -> topicService.get(clusterId(), topicName))
                .onSuccess(topic -> topic.getPartitions().forEach(it -> comboBoxPartitions.getItems().add(String.valueOf(it.getId()))))
                .start();
        // todo add support for select partition
        comboBoxKeyFormat.getSelectionModel().select(consumeFilter.keySerde());
        comboBoxValueFormat.getSelectionModel().select(consumeFilter.valueSerde());
        jsFilterList.clear();
        jsFilterList.addAll(consumeFilter.expressions());
        handleJsFilters();
    }

    private void fillFiltersOnApplyTemplate(ConsumeFilter consumeFilter) {
        comboBoxFromFilter.getSelectionModel().select(consumeFilter.from().type());
        switch (consumeFilter.from().type()) {
            case OFFSET ->
                    consumeFilter.from().offset().ifPresent(it -> textFieldFromOffset.setText(String.valueOf(it)));
            case DATETIME ->
                    consumeFilter.from().timestamp().ifPresent(it -> dateTimePickerFromDatetime.setTimestampValue(it));
            case TIMESTAMP ->
                    consumeFilter.from().timestamp().ifPresent(it -> textFieldFromTimestamp.setText(String.valueOf(it)));
        }
        comboBoxMaxResults.getSelectionModel().select(String.valueOf(consumeFilter.maxResults()));
        if (consumeFilter.partitions().isEmpty()) {
            comboBoxPartitions.getSelectionModel().select(0);
        } else {
            comboBoxPartitions.getSelectionModel().select(consumeFilter.partitions().get(0).toString());
        }
        comboBoxKeyFormat.getSelectionModel().select(consumeFilter.keySerde());
        comboBoxValueFormat.getSelectionModel().select(consumeFilter.valueSerde());
        jsFilterList.clear();
        jsFilterList.addAll(consumeFilter.expressions());
        handleJsFilters();
    }

    @Override
    public void initData() {
        loadTableData();
        loadSummary();
    }

    private void initFilters() {
        textFieldQuickSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTableData());
        textFieldQuickSearch.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::filterTableData));

        comboBoxFromFilter.getItems().setAll(
                ConsumeFilter.From.Type.END,
                ConsumeFilter.From.Type.BEGIN,
                ConsumeFilter.From.Type.OFFSET,
                ConsumeFilter.From.Type.DATETIME,
                ConsumeFilter.From.Type.TIMESTAMP
        );
        comboBoxFromFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(ConsumeFilter.From.Type type) {
                return switch (type) {
                    case BEGIN -> i18nService.get("topicTabConsumeView.showFromBegin");
                    case END -> i18nService.get("topicTabConsumeView.showFromEnd");
                    case OFFSET -> i18nService.get("topicTabConsumeView.showFromOffset");
                    case DATETIME -> i18nService.get("topicTabConsumeView.showFromDatetime");
                    case TIMESTAMP -> i18nService.get("topicTabConsumeView.showFromTimestamp");
                };
            }

            @Override
            public ConsumeFilter.From.Type fromString(String string) {
                return null;
            }
        });
        comboBoxFromFilter.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            paneFromFilter.getChildren().clear();
            switch (newValue) {
                case BEGIN, END -> {
                    loadTableData();
                }
                case OFFSET -> {
                    paneFromFilter.getChildren().add(textFieldFromOffset);
                }
                case DATETIME -> {
                    paneFromFilter.getChildren().add(dateTimePickerFromDatetime);
                }
                case TIMESTAMP -> {
                    paneFromFilter.getChildren().add(textFieldFromTimestamp);
                }
            }
        });

        textFieldFromOffset = new TextField();
        textFieldFromOffset.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(0L));
        textFieldFromOffset.setPrefWidth(180);
        HBox.setMargin(textFieldFromOffset, new Insets(0, 0, 0, 10));
        textFieldFromOffset.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::loadTableData));

        dateTimePickerFromDatetime = new DateTimePicker();
        dateTimePickerFromDatetime.setFormat(settingsService.getTimestampFormat().getShortPattern());
        dateTimePickerFromDatetime.setPrefWidth(180);
        HBox.setMargin(dateTimePickerFromDatetime, new Insets(0, 0, 0, 10));
        dateTimePickerFromDatetime.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::loadTableData));

        textFieldFromTimestamp = new TextField();
        textFieldFromTimestamp.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(Instant.now().toEpochMilli()));
        textFieldFromTimestamp.setPrefWidth(180);
        HBox.setMargin(textFieldFromTimestamp, new Insets(0, 0, 0, 10));
        textFieldFromTimestamp.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::loadTableData));

        comboBoxMaxResults.getItems().addAll("10", "20", "50", "100", "200", "500", "1000");
        comboBoxMaxResults.setOnAction(it -> loadTableData());

        comboBoxPartitions.setOnAction(it -> loadTableData());

        var serdeTypes = List.of(
                SerdeType.AUTO,
                SerdeType.STRING,
                SerdeType.BYTES,
                SerdeType.SHORT,
                SerdeType.INTEGER,
                SerdeType.LONG,
                SerdeType.FLOAT,
                SerdeType.DOUBLE,
                SerdeType.UUID,
                SerdeType.AVRO
        );

        comboBoxKeyFormat.getItems().setAll(serdeTypes);
        comboBoxKeyFormat.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxKeyFormat.setOnAction(it -> loadTableData());

        comboBoxValueFormat.getItems().setAll(serdeTypes);
        comboBoxValueFormat.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxValueFormat.setOnAction(it -> loadTableData());

        themeService.setIcon16(buttonJsFilter, "add.png");
        buttonJsFilter.setOnAction(it -> viewManager.showAddJsFilterView(JavaFXUtils.getStage(it), jsFilter -> {
            jsFilterList.add(jsFilter);
            handleJsFilters();
            loadTableData();
        }));

        themeService.setIcon20(buttonReload, "sync.png");
        buttonReload.setOnAction(it -> loadTableData());

        buttonTemplates.setOnAction(actionEvent ->
                viewManager.showFilterTemplatesView(JavaFXUtils.getStage(actionEvent), topicName, createConsumeFilter(), consumeFilter -> {
                    disableLoadData = true;
                    fillFiltersOnApplyTemplate(consumeFilter);
                    disableLoadData = false;
                    loadTableData();
                })
        );

        buttonExport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.setInitialFileName(String.format("%s_messages.json", topicName));
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showSaveDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> Files.writeString(Path.of(file.getAbsolutePath()), gsonPretty.toJson(tableView.getItems().stream().map(it -> it.getSource().toDto()).toList())))
                    .onSuccess(it -> sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.exported")))
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.error"), it))
                    .start();
        });

        handleJsFilters();
    }

    private void handleJsFilters() {
        paneJsFilters.getChildren().clear();

        if (!jsFilterList.isEmpty()) {
            var label = new Label(i18nService.get("topicTabConsumeView.jsFilters"));
            FlowPane.setMargin(label, new Insets(10, 0, 0, 0));
            paneJsFilters.getChildren().add(label);
        }

        for (int i = 0; i < jsFilterList.size(); i++) {
            int index = i;
            var jsFilter = jsFilterList.get(index);

            var menuItemEnableOrDisable = new MenuItem(i18nService.get(jsFilter.isActive() ? "common.disable" : "common.enable"));
            menuItemEnableOrDisable.setOnAction(it -> {
                jsFilterList.set(index, new ConsumeFilter.Expression(jsFilter.name(), jsFilter.code(), !jsFilter.isActive()));
                handleJsFilters();
                loadTableData();
            });

            var menuItemEdit = new MenuItem(i18nService.get("common.edit"));
            menuItemEdit.setOnAction(it -> viewManager.showEditJsFilterView(JavaFXUtils.getStage(it), jsFilter, updatedJsFilter -> {
                jsFilterList.set(index, updatedJsFilter);
                handleJsFilters();
                loadTableData();
            }));
            var menuItemDelete = new MenuItem(i18nService.get("common.delete"));
            menuItemDelete.setOnAction(it -> {
                jsFilterList.remove(index);
                handleJsFilters();
                loadTableData();
            });

            var menuButton = new MenuButton(StringUtils.isNotBlank(jsFilter.name()) ? jsFilter.name() : jsFilter.code());
            menuButton.setTooltip(JavaFXUtils.tooltip(jsFilter.code()));
            menuButton.setMaxWidth(150);
            menuButton.getStyleClass().addAll("menu-button-js-filter", "menu-button-without-arrow", "secondary-outline");
            if (!jsFilter.isActive()) menuButton.getStyleClass().add("menu-button-js-filter-inactive");
            FlowPane.setMargin(menuButton, new Insets(10, 0, 0, 0));
            menuButton.getItems().addAll(menuItemEnableOrDisable, menuItemEdit, menuItemDelete);

            paneJsFilters.getChildren().add(menuButton);
        }
    }

    private ConsumeFilter createConsumeFilter() {
        var fromType = comboBoxFromFilter.getValue();
        var offset = Optional.<Long>empty();
        var timestamp = Optional.<Long>empty();
        switch (fromType) {
            case OFFSET -> offset = tryOrEmpty(() -> Long.parseLong(textFieldFromOffset.getText()));
            case DATETIME -> timestamp = tryOrEmpty(() -> dateTimePickerFromDatetime.getTimestampValue());
            case TIMESTAMP -> timestamp = tryOrEmpty(() -> Long.parseLong(textFieldFromTimestamp.getText()));
        }
        var from = new ConsumeFilter.From(fromType, offset, timestamp);

        var maxResults = tryOrEmpty(() -> Integer.parseInt(comboBoxMaxResults.getValue())).orElse(100);

        var partitions = new ArrayList<Integer>(1);
        tryOrEmpty(() -> Integer.parseInt(comboBoxPartitions.getValue())).ifPresent(partitions::add);

        var keySerde = comboBoxKeyFormat.getValue();
        var valueSerde = comboBoxValueFormat.getValue();

        return new ConsumeFilter(from, maxResults, partitions, keySerde, valueSerde, new ArrayList<>(jsFilterList));
    }

    private void initTable() {
        columnTimestamp = JavaFXUtils.tableColumn(i18nService.get("common.timestamp"));
        columnTimestamp.setCellValueFactory(it -> it.getValue().timestampProperty());
        columnTimestamp.setComparator(NumberLabel.COMPARATOR);
        columnTimestamp.setPrefWidth(200);

        var columnKey = JavaFXUtils.<RecordModelView, Label>tableColumn(i18nService.get("common.key"));
        columnKey.setCellValueFactory(it -> it.getValue().keyProperty());
        columnKey.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnKey.setMinWidth(200);

        var columnValue = JavaFXUtils.<RecordModelView, Label>tableColumn(i18nService.get("common.value"));
        columnValue.setCellValueFactory(it -> it.getValue().valueProperty());
        columnValue.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnValue.setMinWidth(200);

        var columnPartition = JavaFXUtils.<RecordModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnPartition.setPrefWidth(110);

        var columnOffset = JavaFXUtils.<RecordModelView, NumberLabel>tableColumn(i18nService.get("common.offset"));
        columnOffset.setCellValueFactory(it -> it.getValue().offsetProperty());
        columnOffset.setComparator(NumberLabel.COMPARATOR);
        columnOffset.setPrefWidth(100);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnTimestamp, columnPartition, columnOffset).multiply(0.9);
        columnKey.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnValue.prefWidthProperty().bind(remainTableWidth.multiply(0.6));

        //noinspection unchecked
        tableView.getColumns().addAll(columnTimestamp, columnKey, columnValue, columnPartition, columnOffset);
        JavaFXUtils.disableTableViewFocus(tableView);
        tableView.setRowFactory(JavaFXUtils.clickRowFactory(item ->
                viewManager.showTopicRecordView(JavaFXUtils.getStage(tableView), topicName, item.getSource())
        ));

        var modelSortedList = new SortedList<>(modelFilteredList);
        modelSortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(modelSortedList);

        var tableListProperty = new SimpleListProperty<RecordModelView>();
        tableListProperty.bind(tableView.itemsProperty());
        tableListProperty.emptyProperty().addListener((observable, oldValue, newValue) ->
                labelEmptyTableView.setText(BooleanUtils.isTrue(newValue) ? i18nService.get("common.noData") : "")
        );
        tableListProperty.sizeProperty().addListener((observable, oldValue, newValue) ->
                labelCurrentCountCardConsume.setText(newValue == null ? "0" : newValue.toString())
        );
    }

    private void loadTableData() {
        JavaFXUtils.clearTasks(futureTasks);
        if (disableLoadData) return;

        var consumeFilter = createConsumeFilter();

        boxCardConsume.setVisible(true);
        modelObservableList.clear();
        labelEmptyTableView.setText("");
        progressIndicator.setVisible(true);

        if (tableView.getSortOrder().isEmpty()) {
            columnTimestamp.setSortType(
                    consumeFilter.from().type() == ConsumeFilter.From.Type.END
                            ? TableColumn.SortType.DESCENDING
                            : TableColumn.SortType.ASCENDING
            );
            tableView.getSortOrder().add(columnTimestamp);
        }

        var queue = new ArrayBlockingQueue<Record>(consumeFilter.maxResults());
        var cancel = new AtomicBoolean();

        var consumeRecordsTask = futureTask(() -> recordService.consume(clusterId(), topicName, consumeFilter, queue, cancel), cancel)
                .onError(this::loadDataError)
                .startNow();
        futureTasks.add(consumeRecordsTask);

        var handleConsumedRecordsTask = futureTask(() -> CompletableFuture.runAsync(() -> {
            while (!cancel.get()) {
                Record record;
                try {
                    record = queue.poll(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                if (cancel.get()) break;
                var completed = record == null || record.isLast();
                if (completed) {
                    Platform.runLater(this::onCompletedLoadTableData);
                    break;
                }
                var model = new RecordModelView(record);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    modelObservableList.add(model);
                });
            }
        }))
                .onError(it -> onCompletedLoadTableData())
                .startNow();
        futureTasks.add(handleConsumedRecordsTask);

        buttonCancelConsume.setOnAction(it -> {
            consumeRecordsTask.cancel();
            handleConsumedRecordsTask.cancel();
            onCompletedLoadTableData();
        });
    }

    private void onCompletedLoadTableData() {
        boxCardConsume.setVisible(false);
        progressIndicator.setVisible(false);
        if (modelObservableList.isEmpty()) {
            labelEmptyTableView.setText(i18nService.get("common.noData"));
        }
    }

    private void filterTableData() {
        if (disableLoadData) return;
        modelFilteredList.setPredicate(this::getFilterTableDataPredicate);
    }

    private boolean getFilterTableDataPredicate(RecordModelView model) {
        var search = textFieldQuickSearch.getText();
        if (StringUtils.isBlank(search)) {
            return true;
        }
        if (Strings.CI.contains(model.getSource().getKey(), search)
                || Strings.CI.contains(model.getSource().getValue(), search)) {
            return true;
        }
        return false;
    }

    private void loadSummary() {
        setPaneLoader(themeService.getIconLoader16(), paneCardRecordCountContent, paneCardSizeContent, paneCardPartitionCountContent, paneCardReplicationFactorContent, paneCardCleanupPolicyContent);

        futureTask(() -> topicService.getTopicSummary(clusterId(), topicName))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.recordCount()), paneCardRecordCountContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.partitionCount()), paneCardPartitionCountContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.replicationFactor()), paneCardReplicationFactorContent);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneCardRecordCountContent, paneCardPartitionCountContent, paneCardReplicationFactorContent);
                    loadDataError(it);
                })
                .start();

        futureTask(() -> logDirService.getTopicSummary(clusterId(), topicName))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(FormatUtils.prettySizeInBytes(summary.size())), paneCardSizeContent);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneCardSizeContent);
                    loadDataError(it);
                })
                .start();

        futureTask(() -> configService.getTopicSummary(clusterId(), topicName))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.cleanupPolicy()), paneCardCleanupPolicyContent);
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneCardCleanupPolicyContent);
                    loadDataError(it);
                })
                .start();
    }

    public class RecordModelView {

        private final Record source;
        private final SimpleObjectProperty<NumberLabel> timestamp = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> offset = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> key = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> value = new SimpleObjectProperty<>();

        public RecordModelView(Record source) {
            this.source = source;
            var timestampLabel = numberLabelText(source.getTimestamp(), settingsService.getTimestampFormat().getFullFormatter().format(Instant.ofEpochMilli(source.getTimestamp())), "font-medium");
            timestampLabel.setTooltip(JavaFXUtils.tooltip(String.valueOf(source.getTimestamp())));
            timestamp.set(timestampLabel);
            partition.set(JavaFXUtils.numberLabel(source.getPartition(), "font-code"));
            offset.set(JavaFXUtils.numberLabel(source.getOffset(), "font-code"));
            key.set(JavaFXUtils.labelWithTooltip(source.getKeyCompressed(), JavaFXUtils.tooltip(source.getKeyFormatted()), "font-code"));
            value.set(JavaFXUtils.labelWithTooltip(source.getValueCompressed(), JavaFXUtils.tooltip(source.getValueFormatted()), "font-code"));
        }

        public Record getSource() {
            return source;
        }

        public SimpleObjectProperty<NumberLabel> timestampProperty() {
            return timestamp;
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<NumberLabel> offsetProperty() {
            return offset;
        }

        public SimpleObjectProperty<Label> keyProperty() {
            return key;
        }

        public SimpleObjectProperty<Label> valueProperty() {
            return value;
        }
    }
}
