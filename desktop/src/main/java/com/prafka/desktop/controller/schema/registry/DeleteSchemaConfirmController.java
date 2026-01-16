package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractController;
import jakarta.inject.Inject;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

import java.util.Optional;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;

public class DeleteSchemaConfirmController extends AbstractController {

    public Label labelTitle;
    public Label labelContent;
    public CheckBox checkBoxPermanently;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonConfirm;

    private final SchemaRegistryService schemaRegistryService;
    private String subject;
    private Optional<Integer> version;
    private Runnable onSuccess;

    @Inject
    public DeleteSchemaConfirmController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setData(String subject, Runnable onSuccess) {
        this.subject = subject;
        this.version = Optional.empty();
        this.onSuccess = onSuccess;
    }

    public void setData(String subject, int version, Runnable onSuccess) {
        this.subject = subject;
        this.version = Optional.of(version);
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        buttonCancel.setOnAction(it -> getStage(it).close());
    }

    @Override
    public void initUi() {
        if (version.isPresent()) initializeForSubjectVersion();
        else initializeForSubject();
    }

    @Override
    public void initScene(Scene scene) {
        buttonConfirm.requestFocus();
    }

    @Override
    protected void onEnter() {
        buttonConfirm.fire();
    }

    private void initializeForSubject() {
        labelTitle.setText(i18nService.get("deleteSchemaConfirmView.title"));
        labelContent.setText(String.format(i18nService.get("deleteSchemaConfirmView.content"), subject));
        buttonConfirm.setText(i18nService.get("schema.deleteSchema"));
        buttonConfirm.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicator.setVisible(true);
            buttonConfirm.setDisable(true);
            futureTask(() -> schemaRegistryService.delete(clusterId(), subject, checkBoxPermanently.isSelected()))
                    .onSuccess(it -> {
                        getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicator.setVisible(false);
                        buttonConfirm.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    private void initializeForSubjectVersion() {
        labelTitle.setText(i18nService.get("deleteSchemaVersionConfirmView.title"));
        labelContent.setText(String.format(i18nService.get("deleteSchemaVersionConfirmView.content"), subject, version.get()));
        buttonConfirm.setText(i18nService.get("schema.deleteVersion"));
        buttonConfirm.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicator.setVisible(true);
            buttonConfirm.setDisable(true);
            futureTask(() -> schemaRegistryService.delete(clusterId(), subject, version.get(), checkBoxPermanently.isSelected()))
                    .onSuccess(it -> {
                        getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicator.setVisible(false);
                        buttonConfirm.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }
}
