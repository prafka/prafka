package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.DateTimePicker;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.kafka.common.TopicPartition;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.prafka.core.util.StreamUtils.tryOrEmpty;
import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

/**
 * Controller for the consumer group offset reset dialog.
 *
 * <p>Allows resetting offsets for specific topics/partitions using various strategies:
 * earliest, latest, specific offset, shift by value, datetime, or timestamp.
 * Displays a preview of current and new offsets before applying changes.
 */
public class EditConsumerGroupController extends AbstractController {

    public GridPane paneTopicPartition;
    public Pane paneTopic;
    public ComboBox<String> comboBoxTopic;
    public Pane panePartition;
    public ComboBox<String> comboBoxPartition;
    public GridPane paneOffsetStrategyValue;
    public Pane paneOffsetStrategy;
    public ComboBox<ConsumerGroup.OffsetStrategy> comboBoxOffsetStrategy;
    public Pane paneValue;
    public TextField textFieldSpecificOffset;
    public TextField textFieldShiftBy;
    public DateTimePicker dateTimePickerDatetime;
    public TextField textFieldTimestamp;
    public TableView<PreviewModelView> tableView;
    public Label labelEmptyTableView;
    public ProgressIndicator progressIndicatorTableView;
    public HBox paneAlert;
    public ProgressIndicator progressIndicatorButtonBlock;
    public Button buttonCancel;
    public Button buttonSave;

    private final ConsumerGroupService consumerGroupService;
    private String groupId;
    private Runnable onSuccess;

    @Inject
    public EditConsumerGroupController(ConsumerGroupService consumerGroupService) {
        this.consumerGroupService = consumerGroupService;
    }

    public void setData(String groupId, Runnable onSuccess) {
        this.groupId = groupId;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        comboBoxTopic.getItems().add(i18nService.get("common.all"));
        comboBoxTopic.getSelectionModel().select(0);
        comboBoxTopic.prefWidthProperty().bind(paneTopic.widthProperty());

        comboBoxPartition.prefWidthProperty().bind(panePartition.widthProperty());

        comboBoxOffsetStrategy.getItems().setAll(ConsumerGroup.OffsetStrategy.values());
        comboBoxOffsetStrategy.prefWidthProperty().bind(paneOffsetStrategy.widthProperty());
        comboBoxOffsetStrategy.setConverter(new StringConverter<>() {
            @Override
            public String toString(ConsumerGroup.OffsetStrategy strategy) {
                return switch (strategy) {
                    case EARLIEST -> i18nService.get("common.earliest");
                    case LATEST -> i18nService.get("common.latest");
                    case SPECIFIC -> i18nService.get("common.specificOffset");
                    case SHIFT -> i18nService.get("common.shiftBy");
                    case DATETIME -> i18nService.get("common.datetime");
                    case TIMESTAMP -> i18nService.get("common.timestamp");
                };
            }

            @Override
            public ConsumerGroup.OffsetStrategy fromString(String string) {
                return null;
            }
        });
        comboBoxOffsetStrategy.getSelectionModel().select(0);

        textFieldSpecificOffset.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(0L));
        textFieldShiftBy.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(0L));
        dateTimePickerDatetime.setFormat(settingsService.getTimestampFormat().getShortPattern());
        textFieldTimestamp.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(Instant.now().toEpochMilli()));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());

        initializeTableView();
    }

    @Override
    public void initUi() {
        buttonSave.setDisable(true);
        futureTask(() -> consumerGroupService.get(clusterId(), groupId))
                .onSuccess(group -> {
                    buttonSave.setDisable(false);
                    doInit(group);
                })
                .onError(it -> {
                    loadDataError(Pos.BOTTOM_LEFT, it);
                })
                .start();
    }

    @Override
    protected void onEnter() {
        buttonSave.fire();
    }

    private void doInit(ConsumerGroup consumerGroup) {
        comboBoxTopic.getItems().addAll(consumerGroup.getTopics().stream().sorted().toList());
        comboBoxTopic.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            comboBoxPartition.getItems().clear();
            if (newValue != null && !newValue.equals(i18nService.get("common.all"))) {
                paneTopicPartition.getColumnConstraints().get(0).setPercentWidth(60);
                paneTopicPartition.getColumnConstraints().get(1).setPercentWidth(40);
                paneTopicPartition.setHgap(20);
                panePartition.setVisible(true);
                comboBoxPartition.getItems().add(i18nService.get("common.all"));
                comboBoxPartition.getItems().addAll(
                        consumerGroup.getPartitionOffsets(newValue).keySet().stream()
                                .sorted(Comparator.comparing(TopicPartition::partition))
                                .map(offset -> String.valueOf(offset.partition()))
                                .toList()
                );
                comboBoxPartition.getSelectionModel().select(0);
            } else {
                panePartition.setVisible(false);
                paneTopicPartition.getColumnConstraints().get(0).setPercentWidth(100);
                paneTopicPartition.getColumnConstraints().get(1).setPercentWidth(0);
                paneTopicPartition.setHgap(0);
            }
            calculatePreview(consumerGroup);
        });
        comboBoxPartition.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> calculatePreview(consumerGroup));

        comboBoxOffsetStrategy.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue == ConsumerGroup.OffsetStrategy.EARLIEST || newValue == ConsumerGroup.OffsetStrategy.LATEST) {
                paneValue.setVisible(false);
                textFieldSpecificOffset.setVisible(false);
                textFieldShiftBy.setVisible(false);
                dateTimePickerDatetime.setVisible(false);
                paneOffsetStrategyValue.getColumnConstraints().get(0).setPercentWidth(100);
                paneOffsetStrategyValue.getColumnConstraints().get(1).setPercentWidth(0);
                paneOffsetStrategyValue.setHgap(0);
            } else {
                paneOffsetStrategyValue.getColumnConstraints().get(0).setPercentWidth(60);
                paneOffsetStrategyValue.getColumnConstraints().get(1).setPercentWidth(40);
                paneOffsetStrategyValue.setHgap(20);
                paneValue.setVisible(true);
                textFieldSpecificOffset.setVisible(false);
                textFieldShiftBy.setVisible(false);
                dateTimePickerDatetime.setVisible(false);
                textFieldTimestamp.setVisible(false);
                if (newValue == ConsumerGroup.OffsetStrategy.SPECIFIC) {
                    textFieldSpecificOffset.setVisible(true);
                } else if (newValue == ConsumerGroup.OffsetStrategy.SHIFT) {
                    textFieldShiftBy.setVisible(true);
                } else if (newValue == ConsumerGroup.OffsetStrategy.DATETIME) {
                    dateTimePickerDatetime.setVisible(true);
                } else if (newValue == ConsumerGroup.OffsetStrategy.TIMESTAMP) {
                    textFieldTimestamp.setVisible(true);
                }
            }
            calculatePreview(consumerGroup);
        });
        textFieldSpecificOffset.textProperty().addListener((observable, oldValue, newValue) -> calculatePreview(consumerGroup));
        textFieldShiftBy.textProperty().addListener((observable, oldValue, newValue) -> calculatePreview(consumerGroup));
        dateTimePickerDatetime.dateTimeValueProperty().addListener((observable, oldValue, newValue) -> calculatePreview(consumerGroup));
        textFieldTimestamp.textProperty().addListener((observable, oldValue, newValue) -> calculatePreview(consumerGroup));

        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var offsets = tableView.getItems().stream().collect(Collectors.toMap(PreviewModelView::getTp, it -> it.newOffsetProperty().getValue().getSource().longValue()));

            progressIndicatorButtonBlock.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> consumerGroupService.updateOffsets(clusterId(), groupId, offsets))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonSave.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });

        calculatePreview(consumerGroup);
    }

    private void initializeTableView() {
        var columnTopic = JavaFXUtils.<PreviewModelView, Label>tableColumn(i18nService.get("common.topic"));
        columnTopic.setCellValueFactory(it -> it.getValue().topicProperty());
        columnTopic.setMinWidth(150);

        var columnPartition = JavaFXUtils.<PreviewModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setPrefWidth(120);

        var columnOffsets = JavaFXUtils.<PreviewModelView, Label>tableColumn(i18nService.get("common.offsets"));
        columnOffsets.setCellValueFactory(it -> it.getValue().offsetsProperty());
        columnOffsets.setSortable(false);
        columnOffsets.setPrefWidth(150);

        var columnCurrentOffset = JavaFXUtils.<PreviewModelView, NumberLabel>tableColumn(i18nService.get("common.current"));
        columnCurrentOffset.setId("columnCurrentOffset");
        JavaFXUtils.addTableColumnHeaderTooltip(columnCurrentOffset, i18nService.get("common.currentOffset"));
        columnCurrentOffset.setCellValueFactory(it -> it.getValue().currentOffsetProperty());
        columnCurrentOffset.setPrefWidth(130);

        var columnNewOffset = JavaFXUtils.<PreviewModelView, NumberLabel>tableColumn(i18nService.get("common.new"));
        columnNewOffset.setId("columnNewOffset");
        JavaFXUtils.addTableColumnHeaderTooltip(columnNewOffset, i18nService.get("common.newOffset"));
        columnNewOffset.setCellValueFactory(it -> it.getValue().newOffsetProperty());
        columnNewOffset.setPrefWidth(130);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPartition, columnOffsets, columnCurrentOffset, columnNewOffset).multiply(0.9);
        columnTopic.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnTopic, columnPartition, columnOffsets, columnCurrentOffset, columnNewOffset);
        JavaFXUtils.disableTableViewFocus(tableView);
    }

    private void calculatePreview(ConsumerGroup consumerGroup) {
        var partitionOffsets = new HashMap<TopicPartition, ConsumerGroup.Offset>();
        var topic = comboBoxTopic.getSelectionModel().getSelectedItem();
        if (topic.equals(i18nService.get("common.all"))) {
            partitionOffsets.putAll(consumerGroup.getPartitionOffsets());
        } else {
            var partition = comboBoxPartition.getSelectionModel().getSelectedItem();
            if (partition != null && !partition.equals(i18nService.get("common.all"))) {
                partitionOffsets.putAll(consumerGroup.getPartitionOffset(topic, Integer.parseInt(partition)));
            } else {
                partitionOffsets.putAll(consumerGroup.getPartitionOffsets(topic));
            }
        }

        var strategy = comboBoxOffsetStrategy.getSelectionModel().getSelectedItem();
        var specific = tryOrEmpty(() -> Integer.parseInt(textFieldSpecificOffset.getText()));
        var shift = tryOrEmpty(() -> Integer.parseInt(textFieldShiftBy.getText()));
        var timestamp = Optional.<Long>empty();
        if (strategy == ConsumerGroup.OffsetStrategy.DATETIME) {
            timestamp = tryOrEmpty(() -> dateTimePickerDatetime.getTimestampValue());
        } else if (strategy == ConsumerGroup.OffsetStrategy.TIMESTAMP) {
            timestamp = tryOrEmpty(() -> Long.parseLong(textFieldTimestamp.getText()));
        }
        var filter = new ConsumerGroupService.CalculateNewOffsetsFilter(strategy, specific, shift, timestamp);

        if (strategy == ConsumerGroup.OffsetStrategy.DATETIME || strategy == ConsumerGroup.OffsetStrategy.TIMESTAMP) {
            paneTopicPartition.setDisable(true);
            paneOffsetStrategyValue.setDisable(true);
            tableView.getItems().clear();
            labelEmptyTableView.setText("");
            progressIndicatorTableView.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> consumerGroupService.calculateNewOffsets(clusterId(), groupId, partitionOffsets, filter))
                    .onSuccess(newOffsets -> {
                        var result = newOffsets.entrySet().stream()
                                .map(entry -> new PreviewModelView(entry.getKey(), partitionOffsets.get(entry.getKey()), entry.getValue()))
                                .toList();
                        paneTopicPartition.setDisable(false);
                        paneOffsetStrategyValue.setDisable(false);
                        progressIndicatorTableView.setVisible(false);
                        buttonSave.setDisable(false);
                        if (result.isEmpty()) {
                            labelEmptyTableView.setText(i18nService.get("common.noData"));
                        } else {
                            tableView.getItems().setAll(result);
                        }
                    })
                    .onError(it -> {
                        paneTopicPartition.setDisable(false);
                        paneOffsetStrategyValue.setDisable(false);
                        progressIndicatorTableView.setVisible(false);
                        buttonSave.setDisable(false);
                        labelEmptyTableView.setText(i18nService.get("common.noData"));
                        logError(it);
                    })
                    .start();
        } else {
            paneTopicPartition.setDisable(false);
            paneOffsetStrategyValue.setDisable(false);
            progressIndicatorTableView.setVisible(false);
            buttonSave.setDisable(false);
            labelEmptyTableView.setText("");
            futureTask(() -> consumerGroupService.calculateNewOffsets(clusterId(), groupId, partitionOffsets, filter))
                    .onSuccess(newOffsets -> {
                        var result = newOffsets.entrySet().stream()
                                .map(entry -> new PreviewModelView(entry.getKey(), partitionOffsets.get(entry.getKey()), entry.getValue()))
                                .toList();
                        if (result.isEmpty()) {
                            labelEmptyTableView.setText(i18nService.get("common.noData"));
                        } else {
                            tableView.getItems().setAll(result);
                        }
                    })
                    .onError(it -> {
                        labelEmptyTableView.setText(i18nService.get("common.noData"));
                        logError(it);
                    })
                    .start();
        }
    }

    public static class PreviewModelView {

        private final TopicPartition tp;
        private final SimpleObjectProperty<Label> topic = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> offsets = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> currentOffset = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> newOffset = new SimpleObjectProperty<>();

        public PreviewModelView(TopicPartition tp, ConsumerGroup.Offset offset, long newOffset) {
            this.tp = tp;
            topic.set(JavaFXUtils.labelWithTooltip(tp.topic(), "font-medium"));
            partition.set(JavaFXUtils.numberLabel(tp.partition(), "font-code"));
            offsets.set(JavaFXUtils.label(offset.begin() + " -> " + offset.end(), "font-code"));
            currentOffset.set(JavaFXUtils.numberLabel(offset.current(), "font-code"));
            this.newOffset.set(JavaFXUtils.numberLabel(newOffset, "font-code"));
        }

        public TopicPartition getTp() {
            return tp;
        }

        public SimpleObjectProperty<Label> topicProperty() {
            return topic;
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<Label> offsetsProperty() {
            return offsets;
        }

        public SimpleObjectProperty<NumberLabel> currentOffsetProperty() {
            return currentOffset;
        }

        public SimpleObjectProperty<NumberLabel> newOffsetProperty() {
            return newOffset;
        }
    }
}
