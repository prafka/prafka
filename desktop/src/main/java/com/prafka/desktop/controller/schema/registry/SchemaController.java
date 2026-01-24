package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.EventService;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.FormatUtils.prettyEnum;
import static com.prafka.desktop.util.JavaFXUtils.*;

/**
 * Controller for the individual schema subject detail view with tabbed content.
 *
 * <p>Displays current version, total versions, schema type, and compatibility level.
 * Provides tabs for viewing source code and field structure, with options to edit
 * compatibility and delete the schema.
 */
public class SchemaController extends AbstractController {

    public Label labelH1;
    public Label labelH1Comeback;
    public Button buttonEditCompatibility;
    public Button buttonDelete;
    public Label labelCardCurrentVersionTitle;
    public Pane paneCardCurrentVersionContent;
    public Label labelCardTotalVersionCountTitle;
    public Pane paneCardTotalVersionCountContent;
    public Label labelCardTypeTitle;
    public Pane paneCardTypeContent;
    public Label labelCardCompatibilityTitle;
    public Pane paneCardCompatibilityContent;
    public TabPane tabPane;
    public Tab tabSource;
    public Tab tabStructure;

    private final SchemaRegistryService schemaRegistryService;
    private String subject;
    private CompatibilityLevel compatibility;

    @Inject
    public SchemaController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public void initFxml() {
        labelH1Comeback.setOnMouseClicked(it -> eventService.fire(EventService.DashboardEvent.LOAD_SCHEMA_REGISTRY));

        buttonEditCompatibility.setOnAction(sourceActionEvent ->
                viewManager.showEditSchemaCompatibilityView(getStage(sourceActionEvent), subject, compatibility, () -> {
                    sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.compatibilityEdited"));
                    loadSummary();
                })
        );

        buttonDelete.setOnAction(sourceActionEvent ->
                viewManager.showDeleteSchemaConfirmView(getStage(sourceActionEvent), subject, () -> {
                    sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.schemaDeleted"));
                    eventService.fire(EventService.DashboardEvent.LOAD_SCHEMA_REGISTRY);
                })
        );

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (disableLoadData) return;
            if (tabSource.getId().equals(newValue.getId())) {
                tabSource.setContent(viewManager.loadSchemaTabSourceView(subject, this::loadSummary));
                return;
            }
            if (tabStructure.getId().equals(newValue.getId())) {
                tabStructure.setContent(viewManager.loadSchemaTabStructureView(subject, this::loadSummary));
                return;
            }
        });
    }

    @Override
    public void initUi() {
        labelH1.setText(subject);
        tabPane.getSelectionModel().select(0);
    }

    @Override
    public void initData() {
        Platform.runLater(() -> tabSource.setContent(viewManager.loadSchemaTabSourceView(subject, this::loadSummary)));
        loadSummary();
    }

    private void loadSummary() {
        setPaneLoader(themeService.getIconLoader16(), paneCardCurrentVersionContent, paneCardTotalVersionCountContent, paneCardTypeContent, paneCardCompatibilityContent);
        futureTask(() -> schemaRegistryService.get(clusterId(), subject))
                .onSuccess(schema -> {
                    compatibility = schema.getCompatibility().getLevel();
                    setLabel(label(schema.getVersion()), paneCardCurrentVersionContent);
                    setLabel(label(schema.getVersions().size()), paneCardTotalVersionCountContent);
                    setLabel(label(prettyEnum(schema.getType())), paneCardTypeContent);
                    setLabel(label(prettyEnum(schema.getCompatibility().getLevel())), paneCardCompatibilityContent);
                })
                .onError(it -> {
                    setPaneNA(paneCardCurrentVersionContent, paneCardTotalVersionCountContent, paneCardTypeContent, paneCardCompatibilityContent);
                    loadDataError(it);
                })
                .start();
    }
}
