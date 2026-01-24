package com.prafka.desktop.controller.topic;

import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

/**
 * Controller for the individual topic detail view with tabbed content.
 *
 * <p>Displays topic name and provides tabs for consuming messages, producing messages,
 * viewing partitions, configuration, consumer groups, and ACLs. Supports topic deletion
 * and emptying operations.
 */
public class TopicController extends AbstractController {

    public Label labelH1;
    public Label labelH1Comeback;
    public Button buttonEmptyTopic;
    public Button buttonDeleteTopic;
    public TabPane tabPane;
    public Tab tabConsume;
    public Tab tabProduce;
    public Tab tapPartitions;
    public Tab tabConfiguration;
    public Tab tabConsumerGroups;
    //    public Tab tabSchema;
    public Tab tabAcl;

    private final TopicService topicService;
    private String topicName;

    @Inject
    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void initFxml() {
        labelH1Comeback.setOnMouseClicked(it -> eventService.fire(EventService.DashboardEvent.LOAD_TOPICS));

        buttonEmptyTopic.setOnAction(actionEvent ->
                viewManager.showEmptyTopicConfirmView(JavaFXUtils.getStage(actionEvent), topicName, confirmCallback ->
                        futureTask(() -> topicService.empty(clusterId(), topicName))
                                .onSuccess(it -> {
                                    confirmCallback.onSuccess();
                                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicEmptied"));
                                    loadTab(tabPane.getSelectionModel().getSelectedItem());
                                })
                                .onError(confirmCallback::onError)
                                .start()
                )
        );

        buttonDeleteTopic.setOnAction(actionEvent ->
                viewManager.showDeleteTopicConfirmView(JavaFXUtils.getStage(actionEvent), topicName, confirmCallback ->
                        futureTask(() -> topicService.delete(clusterId(), topicName))
                                .onSuccess(it -> {
                                    confirmCallback.onSuccess();
                                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("topic.topicDeleted"));
                                    eventService.fire(EventService.DashboardEvent.LOAD_TOPICS);
                                })
                                .onError(confirmCallback::onError)
                                .start()
                )
        );

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) loadTab(newValue);
        });
    }

    @Override
    public void initUi() {
        labelH1.setText(topicName);
        tabPane.getSelectionModel().select(0);
    }

    @Override
    public void initData() {
        Platform.runLater(() -> tabConsume.setContent(viewManager.loadTopicTabConsumeView(topicName)));
    }

    private void loadTab(Tab tab) {
        if (disableLoadData) return;
        if (tabConsume.getId().equals(tab.getId())) {
            tabConsume.setContent(viewManager.loadTopicTabConsumeView(topicName));
            return;
        }
        if (tabProduce.getId().equals(tab.getId())) {
            tabProduce.setContent(viewManager.loadTopicTabProducerView(topicName));
            return;
        }
        if (tapPartitions.getId().equals(tab.getId())) {
            tapPartitions.setContent(viewManager.loadTopicTabPartitionsView(topicName));
            return;
        }
        if (tabConfiguration.getId().equals(tab.getId())) {
            tabConfiguration.setContent(viewManager.loadTopicTabConfigurationView(topicName));
            return;
        }
        if (tabConsumerGroups.getId().equals(tab.getId())) {
            tabConsumerGroups.setContent(viewManager.loadTopicTabConsumerGroupView(topicName));
            return;
        }
//        if (tabSchema.getId().equals(tab.getId())) {
//            tabSchema.setContent(new Label("schema"));
//            return;
//        }
        if (tabAcl.getId().equals(tab.getId())) {
            tabAcl.setContent(viewManager.loadTopicTabAclView(topicName));
            return;
        }
    }
}
