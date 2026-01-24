package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.model.Schema;
import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractController;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.controlsfx.control.SegmentedButton;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import java.util.function.Consumer;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.CodeHighlight.*;
import static com.prafka.desktop.util.JavaFXUtils.*;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Controller for the schema creation dialog.
 *
 * <p>Supports creating Avro, JSON Schema, and Protobuf schemas with code editor
 * featuring syntax highlighting. Provides naming strategies for topic-based or
 * custom subject names with code templates for each schema type.
 */
public class CreateSchemaController extends AbstractController {

    public Pane paneContent;
    public RadioButton radioButtonAvroFormat;
    public RadioButton radioButtonJsonFormat;
    public RadioButton radioButtonProtobufFormat;
    public ComboBox<NameStrategy> comboBoxNameStrategy;
    public VBox paneTopicNameStrategy;
    public TextField textFieldNameTopicNameStrategy;
    public VBox paneCustomNameStrategy;
    public TextField textFieldNameCustomNameStrategy;
    public CodeArea codeArea;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonCreate;

    private final SchemaRegistryService schemaRegistryService;
    private Subscription codeHighlightSubscription;
    private Runnable onSuccess;

    @Inject
    public CreateSchemaController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area-border");
        setCodeAreaAutoIntend(codeArea);

        Consumer<Schema.Type> fillCodeArea = type -> {
            if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
            codeArea.getStyleClass().removeAll("code-area-avro", "code-area-json", "code-area-protobuf");
            switch (type) {
                case AVRO -> {
                    codeArea.getStyleClass().add("code-area-avro");
                    codeArea.replaceText(AVRO_TEMPLATE);
                }
                case JSON -> {
                    codeArea.getStyleClass().add("code-area-json");
                    codeArea.replaceText(JSON_TEMPLATE);
                }
                case PROTOBUF -> {
                    codeArea.getStyleClass().add("code-area-protobuf");
                    codeArea.replaceText(PROTOBUF_TEMPLATE);
                }
            }

            task(() -> switch (type) {
                case AVRO -> highlightAvro(codeArea.getText());
                case JSON -> highlightJson(codeArea.getText());
                case PROTOBUF -> highlightProtobuf(codeArea.getText());
            })
                    .onSuccess(it -> codeArea.setStyleSpans(0, it))
                    .start();

            codeHighlightSubscription = codeHighlightSubscription(codeArea, () -> switch (type) {
                case AVRO -> highlightAvro(codeArea.getText());
                case JSON -> highlightJson(codeArea.getText());
                case PROTOBUF -> highlightProtobuf(codeArea.getText());
            });
        };

        fillCodeArea.accept(Schema.Type.AVRO);
        radioButtonAvroFormat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) fillCodeArea.accept(Schema.Type.AVRO);
        });
        radioButtonJsonFormat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) fillCodeArea.accept(Schema.Type.JSON);
        });
        radioButtonProtobufFormat.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) fillCodeArea.accept(Schema.Type.PROTOBUF);
        });

        textFieldNameTopicNameStrategy = new TextField();
        textFieldNameTopicNameStrategy.setDisable(true);
        Runnable fillPaneTopicNameStrategy = () -> {
            var labelKeyOrValue = label(i18nService.get("createSchemaView.keyOrValue"), "font-medium");
            VBox.setMargin(labelKeyOrValue, new Insets(20, 0, 5, 0));

            var textFieldTopic = new TextField();
            textFieldTopic.setPromptText(i18nService.get("common.topicName"));

            var segmentedButton = new SegmentedButton();
            segmentedButton.getToggleGroup().selectedToggleProperty().addListener(buttonToggleGroupListener());
            var toggleButtonKey = new ToggleButton(i18nService.get("common.key"));
            toggleButtonKey.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (isNotTrue(newValue)) return;
                textFieldNameTopicNameStrategy.setText(textFieldTopic.getText() + "-key");
            });
            var toggleButtonValue = new ToggleButton(i18nService.get("common.value"));
            toggleButtonValue.setSelected(true);
            toggleButtonValue.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (isNotTrue(newValue)) return;
                textFieldNameTopicNameStrategy.setText(textFieldTopic.getText() + "-value");
            });
            segmentedButton.getButtons().addAll(toggleButtonKey, toggleButtonValue);

            var boxTopic = new HBox(
                    label(i18nService.get("common.topic"), "font-medium"),
                    label(" *", "font-medium", "font-red")
            );
            VBox.setMargin(boxTopic, new Insets(20, 0, 5, 0));

            textFieldTopic.textProperty().addListener((observable, oldValue, newValue) -> {
                textFieldNameTopicNameStrategy.setText(newValue + (toggleButtonKey.isSelected() ? "-key" : "-value"));
            });

            var labelComputedName = label(i18nService.get("createSchemaView.computedName"), "font-medium");
            VBox.setMargin(labelComputedName, new Insets(20, 0, 5, 0));

            var labelComputedNameDescription = label(i18nService.get("createSchemaView.computedNameDescription"), "label-desc");
            VBox.setMargin(labelComputedNameDescription, new Insets(3, 0, 0, 0));

            textFieldNameTopicNameStrategy.setText("-value");

            paneTopicNameStrategy.getChildren().addAll(labelKeyOrValue, segmentedButton, boxTopic, textFieldTopic, labelComputedName, textFieldNameTopicNameStrategy, labelComputedNameDescription);
        };

        textFieldNameCustomNameStrategy = new TextField();
        Runnable fillPaneCustomNameStrategy = () -> {
            var boxCustomName = new HBox(
                    label(i18nService.get("createSchemaView.customName"), "font-medium"),
                    label(" *", "font-medium", "font-red")
            );
            VBox.setMargin(boxCustomName, new Insets(20, 0, 5, 0));

            var labelCustomNameDescription = label(i18nService.get("createSchemaView.customNameDescription"), "label-desc");
            VBox.setMargin(labelCustomNameDescription, new Insets(3, 0, 0, 0));

            textFieldNameCustomNameStrategy.setText(null);

            paneCustomNameStrategy.getChildren().addAll(boxCustomName, textFieldNameCustomNameStrategy, labelCustomNameDescription);
        };

        comboBoxNameStrategy.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxNameStrategy.setConverter(new StringConverter<>() {
            @Override
            public String toString(NameStrategy strategy) {
                return switch (strategy) {
                    case TOPIC -> i18nService.get("createSchemaView.topicName");
                    case CUSTOM -> i18nService.get("createSchemaView.customName");
                };
            }

            @Override
            public NameStrategy fromString(String string) {
                return null;
            }
        });
        comboBoxNameStrategy.getItems().setAll(NameStrategy.values());
        comboBoxNameStrategy.getSelectionModel().select(0);
        fillPaneTopicNameStrategy.run();
        comboBoxNameStrategy.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            switch (newValue) {
                case TOPIC -> {
                    paneTopicNameStrategy.getChildren().clear();
                    paneCustomNameStrategy.getChildren().clear();
                    fillPaneTopicNameStrategy.run();
                    paneTopicNameStrategy.setVisible(true);
                    paneCustomNameStrategy.setVisible(false);
                }
                case CUSTOM -> {
                    paneTopicNameStrategy.getChildren().clear();
                    paneCustomNameStrategy.getChildren().clear();
                    fillPaneCustomNameStrategy.run();
                    paneTopicNameStrategy.setVisible(false);
                    paneCustomNameStrategy.setVisible(true);
                }
            }
        });

        buttonCancel.setOnAction(it -> getStage(it).close());
        buttonCreate.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var type = Schema.Type.AVRO;
            if (radioButtonJsonFormat.isSelected()) type = Schema.Type.JSON;
            if (radioButtonProtobufFormat.isSelected()) type = Schema.Type.PROTOBUF;

            String subject;
            switch (comboBoxNameStrategy.getSelectionModel().getSelectedItem()) {
                case TOPIC -> {
                    subject = textFieldNameTopicNameStrategy.getText();
                    if (Strings.CI.equalsAny(subject, "-key", "-value")) {
                        sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.topic")));
                        return;
                    }
                }
                case CUSTOM -> {
                    subject = textFieldNameCustomNameStrategy.getText();
                    if (StringUtils.isBlank(subject)) {
                        sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("createSchemaView.customName")));
                        return;
                    }
                }
                default -> {
                    sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.name")));
                    return;
                }
            }

            progressIndicator.setVisible(true);
            buttonCreate.setDisable(true);
            var finalType = type;
            futureTask(() -> schemaRegistryService.create(clusterId(), subject, finalType, codeArea.getText()))
                    .onSuccess(topic -> {
                        getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicator.setVisible(false);
                        buttonCreate.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    protected void onEnter() {
        buttonCreate.fire();
    }

    @Override
    public void close() {
        super.close();
        if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
    }

    public enum NameStrategy {
        TOPIC,
        CUSTOM,
    }

    private static final String AVRO_TEMPLATE = """
            {
                "type": "record",
                "name": "MyRecord",
                "namespace": "com.mycompany",
                "fields" : [
                    {"name": "id", "type": "long"}
                ]
            }
            """;

    private static final String JSON_TEMPLATE = """
            {
                "$id": "https://mycompany.com/myrecord",
                "$schema": "https://json-schema.org/draft/2019-09/schema",
                "type": "object",
                "title": "MyRecord",
                "description": "Json schema for MyRecord",
                "properties": {
                    "id": {
                        "type": "string"
                    },
                    "name": {
                        "type": [ "string", "null" ]
                    }
                },
                "required": [ "id" ],
                "additionalProperties": false
            }
            """;

    private static final String PROTOBUF_TEMPLATE = """
            syntax = "proto3";
            
            message MyRecord {
                int32 id = 1;
                string createdAt = 2;
                string name = 3;
            }
            """;
}
