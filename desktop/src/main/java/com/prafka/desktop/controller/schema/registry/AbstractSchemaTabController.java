package com.prafka.desktop.controller.schema.registry;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.EventService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

import static com.prafka.desktop.util.JavaFXUtils.getStage;

/**
 * Abstract base controller for schema tab views (source and structure).
 *
 * <p>Provides common functionality for version selection, schema editing,
 * and version deletion with callback support for refreshing parent views.
 */
abstract class AbstractSchemaTabController extends AbstractController {

    @FXML
    public Pane paneFilterBlock;
    @FXML
    public ComboBox<String> comboBoxVersion;
    @FXML
    public Button buttonEdit;
    @FXML
    public Button buttonDelete;

    protected boolean disableChangeVersion = false;
    protected String subject;
    protected Runnable onChange;

    public void setData(String subject, Runnable onChange) {
        this.subject = subject;
        this.onChange = onChange;
    }

    @Override
    public void initFxml() {
        comboBoxVersion.setConverter(new StringConverter<>() {
            @Override
            public String toString(String version) {
                return version == null ? null : i18nService.get("common.version") + " " + version;
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        buttonEdit.setOnAction(sourceActionEvent ->
                viewManager.showEditSchemaView(getStage(sourceActionEvent), subject, Integer.parseInt(comboBoxVersion.getSelectionModel().getSelectedItem()), () -> {
                    sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.schemaUpdated"));
                    loadData();
                    onChange.run();
                })
        );

        buttonDelete.setOnAction(sourceActionEvent ->
                viewManager.showDeleteSchemaVersionConfirmView(getStage(sourceActionEvent), subject, Integer.parseInt(comboBoxVersion.getSelectionModel().getSelectedItem()), () -> {
                    sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.versionDeleted"));
                    if (comboBoxVersion.getItems().size() < 2) {
                        eventService.fire(EventService.DashboardEvent.LOAD_SCHEMA_REGISTRY);
                    } else {
                        loadData();
                        onChange.run();
                    }
                })
        );
    }

    protected abstract void loadData();
}
