package com.prafka.desktop.controller.acl;

import com.prafka.core.service.AclService;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SegmentedButton;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class CreateAclForConsumerController extends AbstractController {

    public Pane paneContent;
    public TextField textFieldPrincipal;
    public TextField textFieldHost;
    public SegmentedButton segmentedButtonTopic;
    public ToggleButton toggleButtonTopicExact;
    public ToggleButton toggleButtonTopicPrefixed;
    public CheckComboBox<String> checkComboBoxTopicExact;
    public TextField textFieldTopicPrefixed;
    public ProgressIndicator progressIndicatorTopic;
    public SegmentedButton segmentedButtonConsumerGroup;
    public ToggleButton toggleButtonConsumerGroupExact;
    public ToggleButton toggleButtonConsumerGroupPrefixed;
    public CheckComboBox<String> checkComboBoxConsumerGroupExact;
    public TextField textFieldConsumerGroupPrefixed;
    public ProgressIndicator progressIndicatorConsumerGroup;

    private final TopicService topicService;
    private final ConsumerGroupService consumerGroupService;
    private final AclService aclService;
    private CreateAclController parentController;

    @Inject
    public CreateAclForConsumerController(TopicService topicService, ConsumerGroupService consumerGroupService, AclService aclService) {
        this.topicService = topicService;
        this.consumerGroupService = consumerGroupService;
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

        segmentedButtonConsumerGroup.getToggleGroup().selectedToggleProperty().addListener(JavaFXUtils.buttonToggleGroupListener());
        checkComboBoxConsumerGroupExact.setSkin(JavaFXUtils.checkComboBoxSkin(checkComboBoxConsumerGroupExact));
        checkComboBoxConsumerGroupExact.getStyleClass().clear();
        checkComboBoxConsumerGroupExact.prefWidthProperty().bind(paneContent.widthProperty());
        checkComboBoxConsumerGroupExact.visibleProperty().bind(toggleButtonConsumerGroupExact.selectedProperty());
        textFieldConsumerGroupPrefixed.visibleProperty().bind(toggleButtonConsumerGroupPrefixed.selectedProperty());
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

        checkComboBoxConsumerGroupExact.setDisable(true);
        progressIndicatorConsumerGroup.setVisible(true);
        futureTask(() -> consumerGroupService.getAllGroupIds(clusterId()))
                .onSuccess(groups -> {
                    progressIndicatorConsumerGroup.setVisible(false);
                    checkComboBoxConsumerGroupExact.setDisable(false);
                    checkComboBoxConsumerGroupExact.getItems().setAll(groups);
                })
                .onError(it -> {
                    progressIndicatorConsumerGroup.setVisible(false);
                    checkComboBoxConsumerGroupExact.setDisable(false);
                    logError(it);
                })
                .start();

        parentController.buttonCreate.setOnAction(actionEvent -> {
            parentController.paneAlert.getChildren().clear();

            var principal = textFieldPrincipal.getText();
            var host = textFieldHost.getText();
            var exactTopics = toggleButtonTopicExact.isSelected() ? checkComboBoxTopicExact.getCheckModel().getCheckedItems() : null;
            var prefixedTopic = toggleButtonTopicPrefixed.isSelected() ? textFieldTopicPrefixed.getText() : null;
            var exactConsumerGroups = toggleButtonConsumerGroupExact.isSelected() ? checkComboBoxConsumerGroupExact.getCheckModel().getCheckedItems() : null;
            var prefixedConsumerGroup = toggleButtonConsumerGroupPrefixed.isSelected() ? textFieldConsumerGroupPrefixed.getText() : null;

            parentController.progressIndicator.setVisible(true);
            parentController.buttonCreate.setDisable(true);
            futureTask(() -> aclService.createForConsumer(clusterId(), principal, host, exactTopics, prefixedTopic, exactConsumerGroups, prefixedConsumerGroup))
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
