package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractController;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Function;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.FormatUtils.prettyEnum;
import static com.prafka.desktop.util.JavaFXUtils.getStage;

public class EditSchemaCompatibilityController extends AbstractController {

    public GridPane paneCompatibility;
    public ToggleGroup toggleGroup;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonSave;

    private final SchemaRegistryService schemaRegistryService;
    private String subject;
    private CompatibilityLevel currentCompatibility;
    private Runnable onSuccess;

    @Inject
    public EditSchemaCompatibilityController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    public void setData(String subject, CompatibilityLevel currentCompatibility, Runnable onSuccess) {
        this.subject = subject;
        this.currentCompatibility = currentCompatibility;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        toggleGroup = new ToggleGroup();

        buttonCancel.setOnAction(it -> getStage(it).close());

        buttonSave.setOnAction(actionEvent -> {
            var compatibility = (CompatibilityLevel) toggleGroup.getSelectedToggle().getUserData();
            paneAlert.getChildren().clear();
            progressIndicator.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> schemaRegistryService.updateCompatibility(clusterId(), subject, compatibility))
                    .onSuccess(it -> {
                        getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicator.setVisible(false);
                        buttonSave.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        Function<CompatibilityLevel, RadioButton> createRadioButton = compatibility -> {
            var radioButton = new RadioButton(prettyEnum(compatibility));
            radioButton.getStyleClass().add("card-radio-button");
            radioButton.setUserData(compatibility);
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(radioButton, Priority.ALWAYS);
            GridPane.setMargin(radioButton, new Insets(10));
            if (currentCompatibility != null && compatibility == currentCompatibility)
                toggleGroup.selectToggle(radioButton);
            return radioButton;
        };
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.BACKWARD), 0, 0);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.BACKWARD_TRANSITIVE), 1, 0);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.FORWARD), 0, 1);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.FORWARD_TRANSITIVE), 1, 1);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.FULL), 0, 2);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.FULL_TRANSITIVE), 1, 2);
        paneCompatibility.add(createRadioButton.apply(CompatibilityLevel.NONE), 0, 3);
    }
}
