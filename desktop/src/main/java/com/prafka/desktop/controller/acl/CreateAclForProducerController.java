package com.prafka.desktop.controller.acl;

import com.prafka.core.service.AclService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SegmentedButton;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class CreateAclForProducerController extends AbstractController {

    public Pane paneContent;
    public TextField textFieldPrincipal;
    public TextField textFieldHost;
    public SegmentedButton segmentedButtonTopic;
    public ToggleButton toggleButtonTopicExact;
    public ToggleButton toggleButtonTopicPrefixed;
    public CheckComboBox<String> checkComboBoxTopicExact;
    public TextField textFieldTopicPrefixed;
    public ProgressIndicator progressIndicatorTopic;
    public SegmentedButton segmentedButtonTransactionalId;
    public ToggleButton toggleButtonTransactionalIdExact;
    public ToggleButton toggleButtonTransactionalIdPrefixed;
    public TextField textFieldTransactionalIdExact;
    public TextField textFieldTransactionalIdPrefixed;
    public CheckBox checkBoxIdempotent;

    private final TopicService topicService;
    private final AclService aclService;
    private CreateAclController parentController;

    @Inject
    public CreateAclForProducerController(TopicService topicService, AclService aclService) {
        this.topicService = topicService;
        this.aclService = aclService;
    }

    public void setParentController(CreateAclController parentController) {
        this.parentController = parentController;
    }

    @Override
    public void initFxml() {
        segmentedButtonTopic.getToggleGroup().selectedToggleProperty().addListener(JavaFXUtils.buttonToggleGroupListener());
        checkComboBoxTopicExact.setSkin(JavaFXUtils.checkComboBoxSkin(checkComboBoxTopicExact));
        checkComboBoxTopicExact.getStyleClass().clear();
        checkComboBoxTopicExact.prefWidthProperty().bind(paneContent.widthProperty());
        checkComboBoxTopicExact.visibleProperty().bind(toggleButtonTopicExact.selectedProperty());
        textFieldTopicPrefixed.visibleProperty().bind(toggleButtonTopicPrefixed.selectedProperty());

        segmentedButtonTransactionalId.getToggleGroup().selectedToggleProperty().addListener(JavaFXUtils.buttonToggleGroupListener());
        textFieldTransactionalIdExact.visibleProperty().bind(toggleButtonTransactionalIdExact.selectedProperty());
        textFieldTransactionalIdPrefixed.visibleProperty().bind(toggleButtonTransactionalIdPrefixed.selectedProperty());
    }

    @Override
    public void initUi() {
        parentController.paneAlert.getChildren().clear();

        checkComboBoxTopicExact.setDisable(true);
        progressIndicatorTopic.setVisible(true);
        futureTask(() -> topicService.getAllNames(clusterId()))
                .onSuccess(topics -> {
                    progressIndicatorTopic.setVisible(false);
                    checkComboBoxTopicExact.setDisable(false);
                    checkComboBoxTopicExact.getItems().setAll(topics);
                })
                .onError(it -> {
                    progressIndicatorTopic.setVisible(false);
                    checkComboBoxTopicExact.setDisable(false);
                    logError(it);
                })
                .start();

        parentController.buttonCreate.setOnAction(actionEvent -> {
            parentController.paneAlert.getChildren().clear();

            var principal = textFieldPrincipal.getText();
            var host = textFieldHost.getText();
            var exactTopics = toggleButtonTopicExact.isSelected() ? checkComboBoxTopicExact.getCheckModel().getCheckedItems() : null;
            var prefixedTopic = toggleButtonTopicPrefixed.isSelected() ? textFieldTopicPrefixed.getText() : null;
            var exactTransactionalId = toggleButtonTransactionalIdPrefixed.isSelected() ? textFieldTransactionalIdExact.getText() : null;
            var prefixedTransactionalId = toggleButtonTransactionalIdPrefixed.isSelected() ? textFieldTransactionalIdPrefixed.getText() : null;
            var idempotent = checkBoxIdempotent.isSelected();

            parentController.progressIndicator.setVisible(true);
            parentController.buttonCreate.setDisable(true);
            futureTask(() -> aclService.createForProducer(clusterId(), principal, host, exactTopics, prefixedTopic, exactTransactionalId, prefixedTransactionalId, idempotent))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
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
