package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class CreateConsumerGroupController extends AbstractController {

    public Pane paneContent;
    public TextField textFieldGroupId;
    public ComboBox<ConsumerGroup.OffsetStrategy> comboBoxOffsetStrategy;
    public CheckComboBox<String> checkComboBoxTopics;
    public ProgressIndicator progressIndicatorTopics;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonCreate;

    private final ConsumerGroupService consumerGroupService;
    private final TopicService topicService;
    private Runnable onSuccess;

    @Inject
    public CreateConsumerGroupController(ConsumerGroupService consumerGroupService, TopicService topicService) {
        this.consumerGroupService = consumerGroupService;
        this.topicService = topicService;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        comboBoxOffsetStrategy.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxOffsetStrategy.getItems().setAll(
                ConsumerGroup.OffsetStrategy.EARLIEST,
                ConsumerGroup.OffsetStrategy.LATEST
        );
        comboBoxOffsetStrategy.setConverter(new StringConverter<>() {
            @Override
            public String toString(ConsumerGroup.OffsetStrategy strategy) {
                return switch (strategy) {
                    case EARLIEST -> i18nService.get("common.earliest");
                    case LATEST -> i18nService.get("common.latest");
                    default -> throw new IllegalArgumentException();
                };
            }

            @Override
            public ConsumerGroup.OffsetStrategy fromString(String string) {
                return null;
            }
        });

        checkComboBoxTopics.setSkin(JavaFXUtils.checkComboBoxSkin(checkComboBoxTopics));
        checkComboBoxTopics.getStyleClass().clear();
        checkComboBoxTopics.prefWidthProperty().bind(paneContent.widthProperty());

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonCreate.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var groupId = textFieldGroupId.getText();
            if (StringUtils.isBlank(groupId)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.name")));
                return;
            }

            var offsetStrategy = comboBoxOffsetStrategy.getSelectionModel().getSelectedItem();

            var topics = checkComboBoxTopics.getCheckModel().getCheckedItems();
            if (CollectionUtils.isEmpty(topics)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.selectTopics")));
                return;
            }

            progressIndicator.setVisible(true);
            buttonCreate.setDisable(true);
            futureTask(() -> consumerGroupService.create(clusterId(), groupId, offsetStrategy, topics))
                    .onSuccess(topic -> {
                        JavaFXUtils.getStage(actionEvent).close();
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
    public void initUi() {
        comboBoxOffsetStrategy.getSelectionModel().select(0);

        checkComboBoxTopics.setDisable(true);
        progressIndicatorTopics.setVisible(true);
        futureTask(() -> topicService.getAllNames(clusterId()))
                .onSuccess(topics -> {
                    progressIndicatorTopics.setVisible(false);
                    checkComboBoxTopics.setDisable(false);
                    checkComboBoxTopics.getItems().setAll(topics);
                })
                .onError(it -> {
                    progressIndicatorTopics.setVisible(false);
                    checkComboBoxTopics.setDisable(false);
                    logError(it);
                })
                .start();
    }

    @Override
    protected void onEnter() {
        buttonCreate.fire();
    }
}
