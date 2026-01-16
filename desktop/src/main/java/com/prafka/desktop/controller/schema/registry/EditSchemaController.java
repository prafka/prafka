package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.model.Schema;
import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractController;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.CodeHighlight.*;
import static com.prafka.desktop.util.FormatUtils.prettyAvro;
import static com.prafka.desktop.util.FormatUtils.prettyJson;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static com.prafka.desktop.util.JavaFXUtils.setCodeAreaAutoIntend;

public class EditSchemaController extends AbstractController {

    public CodeArea codeArea;
    public ProgressIndicator progressIndicator;
    public HBox paneAlert;
    public ProgressIndicator progressIndicatorButtonBlock;
    public Button buttonCancel;
    public Button buttonCheckCompatibility;
    public Button buttonSave;

    private final SchemaRegistryService schemaRegistryService;
    private Subscription codeHighlightSubscription;
    private String subject;
    private int version;
    private Runnable onSuccess;

    @Inject
    public EditSchemaController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setData(String subject, int version, Runnable onSuccess) {
        this.subject = subject;
        this.version = version;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        setCodeAreaAutoIntend(codeArea);

        buttonCancel.setOnAction(it -> getStage(it).close());
    }

    @Override
    public void initUi() {
        progressIndicator.setVisible(true);
        codeArea.setEditable(false);
        buttonCheckCompatibility.setDisable(true);
        buttonSave.setDisable(true);
        futureTask(() -> schemaRegistryService.get(clusterId(), subject, version))
                .onSuccess(schema -> {
                    progressIndicator.setVisible(false);
                    codeArea.setEditable(true);
                    buttonCheckCompatibility.setDisable(false);
                    buttonSave.setDisable(false);
                    doInit(schema);
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    loadDataError(Pos.BOTTOM_LEFT, it);
                })
                .start();
    }

    private void doInit(Schema schema) {
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

        codeHighlightSubscription = codeHighlightSubscription(codeArea, () -> switch (schema.getType()) {
            case AVRO -> highlightAvro(codeArea.getText());
            case JSON -> highlightJson(codeArea.getText());
            case PROTOBUF -> highlightProtobuf(codeArea.getText());
        });

        buttonCheckCompatibility.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicatorButtonBlock.setVisible(true);
            buttonCheckCompatibility.setDisable(true);
            buttonSave.setDisable(true);
            futureTask(() -> schemaRegistryService.checkCompatibility(clusterId(), schema.getSubject(), schema.getType(), codeArea.getText()))
                    .onSuccess(result -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonCheckCompatibility.setDisable(false);
                        buttonSave.setDisable(false);
                        sceneService.addLabelSuccess(paneAlert, i18nService.get("editSchemaView.labelSuccessCheckCompatibility"));
                    })
                    .onError(throwable -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonCheckCompatibility.setDisable(false);
                        buttonSave.setDisable(false);
                        sceneService.addLabelError(paneAlert, i18nService.get("editSchemaView.labelFailedCheckCompatibility"));
                        var root = ExceptionUtils.getRootCause(throwable);
                        if (root instanceof SchemaRegistryService.SchemaNotCompatibleException ex) {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, String.join("\n\n", ex.getErrors()));
                        } else {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, throwable);
                        }
                        logError(throwable);
                    })
                    .start();
        });

        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicatorButtonBlock.setVisible(true);
            buttonCheckCompatibility.setDisable(true);
            buttonSave.setDisable(true);
            futureTask(() -> schemaRegistryService.checkCompatibility(clusterId(), schema.getSubject(), schema.getType(), codeArea.getText()).thenCompose(it -> schemaRegistryService.update(clusterId(), schema.getSubject(), schema.getType(), codeArea.getText())))
                    .onSuccess(it -> {
                        getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonCheckCompatibility.setDisable(false);
                        buttonSave.setDisable(false);
                        sceneService.addLabelError(paneAlert, i18nService.get("common.error"));
                        var root = ExceptionUtils.getRootCause(throwable);
                        if (root instanceof SchemaRegistryService.SchemaNotCompatibleException ex) {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, String.join("\n\n", ex.getErrors()));
                        } else {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, throwable);
                        }
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void close() {
        super.close();
        if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
    }
}
