package com.prafka.desktop.controller.topic;

import com.prafka.core.model.NewRecord;
import com.prafka.core.model.Record;
import com.prafka.core.model.SerdeType;
import com.prafka.core.service.RecordRandomService;
import com.prafka.core.service.RecordService;
import com.prafka.core.service.SchemaRegistryService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.prafka.core.util.StreamUtils.tryOrEmpty;
import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.numberLabelText;

public class TopicTabProduceController extends AbstractController {

    public ComboBox<SerdeType> comboBoxKeyFormat;
    public ComboBox<String> comboBoxKeySchema;
    public Button buttonGenerateKey;
    public CodeArea codeAreaKey;
    public ComboBox<SerdeType> comboBoxValueFormat;
    public ComboBox<String> comboBoxValueSchema;
    public Button buttonGenerateValue;
    public CodeArea codeAreaValue;
    public Pane paneHeaders;
    public Button buttonAddHeader;
    public ComboBox<String> comboBoxPartition;
    public ProgressIndicator progressIndicatorPartition;
    public ComboBox<NewRecord.CompressionType> comboBoxCompression;
    public ComboBox<NewRecord.Asks> comboBoxAsks;
    public CheckBox checkBoxIdempotence;
    public TableView<RecordModelView> tableViewProducedRecords;
    public Label labelEmptyTableView;
    public Button buttonProduce;
    public ProgressIndicator progressIndicatorProduce;
    public Pane paneAlert;

    private final TopicService topicService;
    private final SchemaRegistryService schemaRegistryService;
    private final RecordService recordService;
    private final RecordRandomService recordRandomService;
    private final List<HeaderModelView> headerModelList = new ArrayList<>();
    private String topicName;

    @Inject
    public TopicTabProduceController(TopicService topicService, SchemaRegistryService schemaRegistryService, RecordService recordService, RecordRandomService recordRandomService) {
        this.topicService = topicService;
        this.schemaRegistryService = schemaRegistryService;
        this.recordService = recordService;
        this.recordRandomService = recordRandomService;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void initFxml() {
        initKey();
        initValue();
        initHeaders();
        initAdditionalProperties();
        initFlow();
        initProducedRecords();

        buttonProduce.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var record = new NewRecord();
            record.setKey(comboBoxKeyFormat.getValue() == SerdeType.NULL ? "null" : codeAreaKey.getText());
            record.setKeySerde(comboBoxKeyFormat.getValue());
            if (comboBoxKeyFormat.getValue() == SerdeType.SCHEMA_REGISTRY) {
                if (StringUtils.isBlank(comboBoxKeySchema.getValue())) {
                    sceneService.addLabelError(paneAlert, i18nService.get("topicTabProduceView.selectSchema"));
                    return;
                }
                record.setKeySchemaSubject(Optional.of(comboBoxKeySchema.getValue()));
            }
            record.setValue(comboBoxValueFormat.getValue() == SerdeType.NULL ? "null" : codeAreaValue.getText());
            record.setValueSerde(comboBoxValueFormat.getValue());
            if (comboBoxValueFormat.getValue() == SerdeType.SCHEMA_REGISTRY) {
                if (StringUtils.isBlank(comboBoxValueSchema.getValue())) {
                    sceneService.addLabelError(paneAlert, i18nService.get("topicTabProduceView.selectSchema"));
                    return;
                }
                record.setValueSchemaSubject(Optional.of(comboBoxValueSchema.getValue()));
            }
            record.setHeaders(
                    headerModelList.stream()
                            .filter(it -> StringUtils.isNotBlank(it.getKey().getText()))
                            .map(it -> Map.entry(it.getKey().getText(), it.getValue().getText()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
            record.setPartition(tryOrEmpty(() -> Integer.parseInt(comboBoxPartition.getValue())));
            record.setCompression(comboBoxCompression.getValue());
            record.setAsks(comboBoxAsks.getValue());
            record.setIdempotence(checkBoxIdempotence.isSelected());

            progressIndicatorProduce.setVisible(true);
            buttonProduce.setDisable(true);
            futureTask(() -> recordService.produce(clusterId(), topicName, record))
                    .onSuccess(producedRecord -> {
                        progressIndicatorProduce.setVisible(false);
                        buttonProduce.setDisable(false);
                        tableViewProducedRecords.getItems().add(new RecordModelView(producedRecord));
                    })
                    .onError(throwable -> {
                        progressIndicatorProduce.setVisible(false);
                        buttonProduce.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        var serdeTypes = new LinkedList<>(List.of(
                SerdeType.STRING,
                SerdeType.JSON,
                SerdeType.UUID,
                SerdeType.SHORT,
                SerdeType.INTEGER,
                SerdeType.LONG,
                SerdeType.FLOAT,
                SerdeType.DOUBLE,
                SerdeType.NULL
        ));
        if (sessionService.getCluster().isSchemaRegistryDefined()) {
            serdeTypes.add(1, SerdeType.SCHEMA_REGISTRY);
        }
        comboBoxKeyFormat.getItems().setAll(serdeTypes);
        comboBoxKeyFormat.getSelectionModel().select(0);
        comboBoxValueFormat.getItems().setAll(serdeTypes);
        comboBoxValueFormat.getSelectionModel().select(0);

        codeAreaKey.clear();
        codeAreaValue.clear();

        comboBoxPartition.getItems().clear();
        comboBoxPartition.getItems().add(i18nService.get("common.auto"));
        comboBoxPartition.getSelectionModel().select(0);
        comboBoxPartition.setDisable(true);
        progressIndicatorPartition.setVisible(true);
        futureTask(() -> topicService.get(clusterId(), topicName))
                .onSuccess(topic -> {
                    progressIndicatorPartition.setVisible(false);
                    comboBoxPartition.setDisable(false);
                    topic.getPartitions().forEach(it -> comboBoxPartition.getItems().add(String.valueOf(it.getId())));
                })
                .onError(it -> {
                    progressIndicatorPartition.setVisible(false);
                    comboBoxPartition.setDisable(false);
                    logError(it);
                })
                .start();

        comboBoxCompression.getSelectionModel().select(0);
        comboBoxAsks.getSelectionModel().select(0);

        comboBoxKeySchema.getItems().clear();
        comboBoxValueSchema.getItems().clear();
        futureTask(() -> schemaRegistryService.getAllSubjects(clusterId()))
                .onSuccess(subjects -> {
                    var sortedSubjects = subjects.stream().sorted().toList();
                    var keySchemaIndex = -1;
                    var valueSchemaIndex = -1;
                    for (int i = 0; i < sortedSubjects.size(); i++) {
                        if (Strings.CS.equals(topicName + "-key", sortedSubjects.get(i))) keySchemaIndex = i;
                        if (Strings.CS.equals(topicName + "-value", sortedSubjects.get(i))) valueSchemaIndex = i;
                    }
                    comboBoxKeySchema.getItems().setAll(sortedSubjects);
                    comboBoxKeySchema.getSelectionModel().select(keySchemaIndex);
                    comboBoxValueSchema.getItems().setAll(sortedSubjects);
                    comboBoxValueSchema.getSelectionModel().select(valueSchemaIndex);
                })
                .start();

        paneHeaders.getChildren().clear();
        headerModelList.clear();
        new HeaderModelView();

        tableViewProducedRecords.getItems().clear();

        paneAlert.getChildren().clear();
    }

    private void initKey() {
        comboBoxKeyFormat.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxKeyFormat.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            comboBoxKeySchema.setVisible(newValue == SerdeType.SCHEMA_REGISTRY);
            buttonGenerateKey.setVisible(newValue != SerdeType.SCHEMA_REGISTRY && newValue != SerdeType.NULL);
        });

        comboBoxKeySchema.setButtonCell(JavaFXUtils.comboBoxWithPromptText(comboBoxKeySchema));

        buttonGenerateKey.setOnAction(actionEvent -> {
            buttonGenerateKey.setDisable(true);
            task(() -> recordRandomService.randomKey(comboBoxKeyFormat.getValue()))
                    .onSuccess(random -> {
                        if (comboBoxKeyFormat.getValue() == SerdeType.JSON) {
                            codeAreaKey.replaceText(FormatUtils.prettyJson(random));
                            task(() -> CodeHighlight.highlightJson(codeAreaKey.getText()))
                                    .onSuccess(it -> codeAreaKey.setStyleSpans(0, it))
                                    .start();
                        } else {
                            codeAreaKey.replaceText(random);
                        }
                        buttonGenerateKey.setDisable(false);
                    })
                    .onError(it -> buttonGenerateKey.setDisable(false))
                    .start();
        });

        codeAreaKey.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaKey));
        codeAreaKey.getStyleClass().addAll("code-area-json", "code-area-border");
        JavaFXUtils.setCodeAreaAutoIntend(codeAreaKey);
        CodeHighlight.codeHighlightSubscription(codeAreaKey, () -> {
            var serde = comboBoxKeyFormat.getValue();
            if (serde == SerdeType.JSON || serde == SerdeType.SCHEMA_REGISTRY) {
                return CodeHighlight.highlightJson(codeAreaKey.getText());
            }
            return CodeHighlight.highlightDefault(codeAreaKey.getText());
        });
    }

    private void initValue() {
        comboBoxValueFormat.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxValueFormat.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            comboBoxValueSchema.setVisible(newValue == SerdeType.SCHEMA_REGISTRY);
            buttonGenerateValue.setVisible(newValue != SerdeType.SCHEMA_REGISTRY && newValue != SerdeType.NULL);
        });

        comboBoxValueSchema.setButtonCell(JavaFXUtils.comboBoxWithPromptText(comboBoxKeySchema));

        buttonGenerateValue.setOnAction(actionEvent -> {
            buttonGenerateValue.setDisable(true);
            task(() -> recordRandomService.randomValue(comboBoxValueFormat.getValue()))
                    .onSuccess(random -> {
                        if (comboBoxValueFormat.getValue() == SerdeType.JSON) {
                            codeAreaValue.replaceText(FormatUtils.prettyJson(random));
                            task(() -> CodeHighlight.highlightJson(codeAreaValue.getText()))
                                    .onSuccess(it -> codeAreaValue.setStyleSpans(0, it))
                                    .start();
                        } else {
                            codeAreaValue.replaceText(random);
                        }
                        buttonGenerateValue.setDisable(false);
                    })
                    .onError(it -> buttonGenerateValue.setDisable(false))
                    .start();
        });

        codeAreaValue.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaValue));
        codeAreaValue.getStyleClass().addAll("code-area-json", "code-area-border");
        JavaFXUtils.setCodeAreaAutoIntend(codeAreaValue);
        CodeHighlight.codeHighlightSubscription(codeAreaValue, () -> {
            var serde = comboBoxValueFormat.getValue();
            if (serde == SerdeType.JSON || serde == SerdeType.SCHEMA_REGISTRY) {
                return CodeHighlight.highlightJson(codeAreaValue.getText());
            }
            return CodeHighlight.highlightDefault(codeAreaValue.getText());
        });
    }

    private void initHeaders() {
        themeService.setIcon16(buttonAddHeader, "add.png");
        buttonAddHeader.setOnAction(it -> new HeaderModelView());
    }

    private void initFlow() {
        // todo add support for flow
    }

    private void initAdditionalProperties() {
        comboBoxCompression.getItems().setAll(NewRecord.CompressionType.values());
        comboBoxCompression.setConverter(JavaFXUtils.prettyEnumStringConverter());

        comboBoxAsks.getItems().setAll(NewRecord.Asks.ALL, NewRecord.Asks.LEADER, NewRecord.Asks.NONE);
        comboBoxAsks.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxAsks.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue != NewRecord.Asks.ALL) checkBoxIdempotence.setSelected(false);
        });

        checkBoxIdempotence.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isTrue(newValue)) comboBoxAsks.getSelectionModel().select(0);
        });
    }

    private void initProducedRecords() {
        var columnTimestamp = JavaFXUtils.<RecordModelView, NumberLabel>tableColumn(i18nService.get("common.timestamp"));
        columnTimestamp.setCellValueFactory(it -> it.getValue().timestampProperty());
        columnTimestamp.setSortable(false);
        columnTimestamp.setMinWidth(150);

        var columnOffset = JavaFXUtils.<RecordModelView, NumberLabel>tableColumn(i18nService.get("common.offset"));
        columnOffset.setCellValueFactory(it -> it.getValue().offsetProperty());
        columnOffset.setSortable(false);
        columnOffset.setMinWidth(100);

        var columnPartition = JavaFXUtils.<RecordModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setSortable(false);
        columnPartition.setMinWidth(110);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableViewProducedRecords).multiply(0.9);
        columnTimestamp.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnPartition.prefWidthProperty().bind(remainTableWidth.multiply(0.3));
        columnOffset.prefWidthProperty().bind(remainTableWidth.multiply(0.3));

        //noinspection unchecked
        tableViewProducedRecords.getColumns().addAll(columnTimestamp, columnOffset, columnPartition);
        JavaFXUtils.disableTableViewFocus(tableViewProducedRecords);

        tableViewProducedRecords.setRowFactory(JavaFXUtils.clickRowFactory(item ->
                viewManager.showTopicRecordView(JavaFXUtils.getStage(tableViewProducedRecords), topicName, item.getSource())
        ));
    }

    public class HeaderModelView {

        private final TextField key = new TextField();
        private final TextField value = new TextField();

        public HeaderModelView() {
            key.setPromptText(i18nService.get("common.key"));
            key.getStyleClass().add("sm");
            value.setPromptText(i18nService.get("common.value"));
            value.getStyleClass().add("sm");
            var pane = new GridPane();
            var delete = new Button();
            delete.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(delete, Priority.ALWAYS);
            delete.getStyleClass().addAll("secondary-outline", "button-icon-only");
            themeService.setIcon16(delete, "trash.png");
            delete.setOnAction(it -> {
                headerModelList.remove(this);
                paneHeaders.getChildren().remove(pane);
            });
            pane.getColumnConstraints().addAll(
                    new ColumnConstraints() {{
                        setPercentWidth(45);
                        setHgrow(Priority.ALWAYS);
                    }},
                    new ColumnConstraints() {{
                        setPercentWidth(45);
                        setHgrow(Priority.ALWAYS);
                    }},
                    new ColumnConstraints() {{
                        setPercentWidth(10);
                        setHgrow(Priority.ALWAYS);
                    }}
            );
            pane.add(key, 0, 0);
            GridPane.setMargin(key, new Insets(0, 5, 0, 0));
            pane.add(value, 1, 0);
            GridPane.setMargin(value, new Insets(0, 5, 0, 5));
            pane.add(new VBox(delete) {{
                GridPane.setMargin(this, new Insets(0, 0, 0, 5));
            }}, 2, 0);
            headerModelList.add(this);
            paneHeaders.getChildren().add(pane);
        }

        public TextField getKey() {
            return key;
        }

        public TextField getValue() {
            return value;
        }
    }

    public class RecordModelView {

        private final Record source;
        private final SimpleObjectProperty<NumberLabel> timestamp = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> offset = new SimpleObjectProperty<>();

        public RecordModelView(Record source) {
            this.source = source;
            var timestampLabel = numberLabelText(source.getTimestamp(), settingsService.getTimestampFormat().getShortFormatter().format(Instant.ofEpochMilli(source.getTimestamp())), "font-medium");
            timestampLabel.setTooltip(JavaFXUtils.tooltip(String.valueOf(source.getTimestamp())));
            timestamp.set(timestampLabel);
            partition.set(JavaFXUtils.numberLabel(source.getPartition(), "font-code"));
            offset.set(JavaFXUtils.numberLabel(source.getOffset(), "font-code"));
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
    }
}
