package com.prafka.desktop.controller.acl;

import com.prafka.core.service.AclService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.controlsfx.control.SegmentedButton;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;

/**
 * Controller for creating custom ACLs with full control over permissions.
 *
 * <p>Provides fine-grained ACL configuration allowing selection of resource type,
 * pattern (exact or prefixed), operation, and permission type (allow or deny).
 * Used for advanced ACL scenarios not covered by consumer or producer presets.
 */
public class CreateAclForCustomNeedController extends AbstractController {

    public Pane paneContent;
    public TextField textFieldPrincipal;
    public TextField textFieldHost;
    public ComboBox<ResourceType> comboBoxResourceType;
    public SegmentedButton segmentedButtonOperation;
    public ToggleButton toggleButtonOperationAllow;
    public ToggleButton toggleButtonOperationDeny;
    public ComboBox<AclOperation> comboBoxOperation;
    public VBox boxPattern;
    public SegmentedButton segmentedButtonPattern;
    public ToggleButton toggleButtonPatternExact;
    public ToggleButton toggleButtonPatternPrefixed;
    public TextField textFieldPatternExact;
    public TextField textFieldPatternPrefixed;

    private final AclService aclService;
    private CreateAclController parentController;

    @Inject
    public CreateAclForCustomNeedController(AclService aclService) {
        this.aclService = aclService;
    }

    public void setParentController(CreateAclController parentController) {
        this.parentController = parentController;
    }

    @Override
    public void initFxml() {
        comboBoxResourceType.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxResourceType.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxResourceType.getItems().setAll(aclService.getResourceToOperations().keySet());
        comboBoxResourceType.getSelectionModel().select(0);

        segmentedButtonOperation.getToggleGroup().selectedToggleProperty().addListener(JavaFXUtils.buttonToggleGroupListener());
        comboBoxOperation.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxOperation.setConverter(JavaFXUtils.prettyEnumStringConverter());
        comboBoxOperation.getItems().setAll(aclService.getOperationsByResource(ResourceType.TOPIC));
        comboBoxOperation.getSelectionModel().select(0);
        comboBoxResourceType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                comboBoxOperation.getItems().clear();
            } else {
                comboBoxOperation.getItems().setAll(aclService.getOperationsByResource(newValue));
                comboBoxOperation.getSelectionModel().select(0);
            }
        });

        boxPattern.setVisible(true);
        comboBoxResourceType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                boxPattern.setVisible(false);
            } else {
                boxPattern.setVisible(newValue != ResourceType.CLUSTER);
            }
        });

        segmentedButtonPattern.getToggleGroup().selectedToggleProperty().addListener(JavaFXUtils.buttonToggleGroupListener());
        textFieldPatternExact.visibleProperty().bind(toggleButtonPatternExact.selectedProperty());
        textFieldPatternPrefixed.visibleProperty().bind(toggleButtonPatternPrefixed.selectedProperty());
    }

    @Override
    public void initUi() {
        parentController.paneAlert.getChildren().clear();
        parentController.buttonCreate.setOnAction(actionEvent -> {
            parentController.paneAlert.getChildren().clear();

            var principal = textFieldPrincipal.getText();
            var host = textFieldHost.getText();
            var resource = comboBoxResourceType.getSelectionModel().getSelectedItem();
            var permission = toggleButtonOperationAllow.isSelected() ? AclPermissionType.ALLOW : AclPermissionType.DENY;
            var operation = comboBoxOperation.getSelectionModel().getSelectedItem();
            var patternType = boxPattern.isVisible() ? (toggleButtonPatternExact.isSelected() ? PatternType.LITERAL : PatternType.PREFIXED) : null;
            var patternValue = boxPattern.isVisible() ? (toggleButtonPatternExact.isSelected() ? textFieldPatternExact.getText() : textFieldPatternPrefixed.getText()) : null;

            parentController.progressIndicator.setVisible(true);
            parentController.buttonCreate.setDisable(true);
            futureTask(() -> aclService.create(clusterId(), principal, host, resource, permission, operation, patternType, patternValue))
                    .onSuccess(it -> {
                        getStage(actionEvent).close();
                        parentController.onSuccess.run();
                    })
                    .onError(throwable -> {
                        parentController.progressIndicator.setVisible(false);
                        parentController.buttonCreate.setDisable(false);
                        sceneService.addPaneAlertError(parentController.paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }
}
