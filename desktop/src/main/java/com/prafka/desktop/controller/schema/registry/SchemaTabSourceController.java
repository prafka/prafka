package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.model.Schema;
import com.prafka.core.service.SchemaRegistryService;
import jakarta.inject.Inject;
import javafx.scene.control.ProgressIndicator;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.CodeHighlight.*;
import static com.prafka.desktop.util.FormatUtils.prettyAvro;
import static com.prafka.desktop.util.FormatUtils.prettyJson;

public class SchemaTabSourceController extends AbstractSchemaTabController {

    public CodeArea codeArea;
    public ProgressIndicator progressIndicator;

    private final SchemaRegistryService schemaRegistryService;

    @Inject
    public SchemaTabSourceController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        comboBoxVersion.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null && !disableChangeVersion) {
                codeArea.clear();
                progressIndicator.setVisible(true);
                futureTask(() -> schemaRegistryService.get(clusterId(), subject, Integer.parseInt(newValue)))
                        .onSuccess(schema -> {
                            progressIndicator.setVisible(false);
                            fillCodeArea(schema);
                        })
                        .onError(it -> {
                            progressIndicator.setVisible(false);
                            loadDataError(it);
                        })
                        .start();
            }
        }));

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    }

    @Override
    public void initData() {
        loadData();
    }

    @Override
    protected void loadData() {
        progressIndicator.setVisible(true);
        futureTask(() -> schemaRegistryService.get(clusterId(), subject))
                .onSuccess(schema -> {
                    progressIndicator.setVisible(false);
                    fillCodeArea(schema);
                    disableChangeVersion = true;
                    comboBoxVersion.getItems().setAll(schema.getVersions().stream().map(String::valueOf).toList());
                    comboBoxVersion.getSelectionModel().select(schema.getVersions().size() - 1);
                    disableChangeVersion = false;
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    loadDataError(it);
                })
                .start();
    }

    private void fillCodeArea(Schema schema) {
        var source = switch (schema.getType()) {
            case AVRO -> prettyAvro(schema.getRaw());
            case JSON -> prettyJson(schema.getRaw());
            case PROTOBUF -> schema.getRaw();
        };
        var style = switch (schema.getType()) {
            case AVRO -> "code-area-avro";
            case JSON -> "code-area-json";
            case PROTOBUF -> "code-area-protobuf";
        };
        codeArea.replaceText(source);
        codeArea.getStyleClass().add(style);
        task(() -> switch (schema.getType()) {
            case AVRO -> highlightAvro(source);
            case JSON -> highlightJson(source);
            case PROTOBUF -> highlightProtobuf(source);
        })
                .onSuccess(it -> codeArea.setStyleSpans(0, it))
                .start();
    }
}
