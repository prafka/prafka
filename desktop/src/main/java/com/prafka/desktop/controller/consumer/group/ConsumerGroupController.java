package com.prafka.desktop.controller.consumer.group;

import com.prafka.core.model.ConsumerGroup;
import com.prafka.core.service.ConsumerGroupService;
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
import javafx.scene.layout.Pane;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.FormatUtils.prettyEnum;
import static com.prafka.desktop.util.JavaFXUtils.labelWithTooltip;

/**
 * Controller for the individual consumer group detail view with tabbed content.
 *
 * <p>Displays group state, member count, coordinator, topic/partition counts, and overall lag.
 * Provides tabs for viewing members, topics, and ACLs. Supports offset reset and group deletion
 * when the group is in an editable state (empty or dead).
 */
public class ConsumerGroupController extends AbstractController {

    public Label labelH1;
    public Label labelH1Comeback;
    public Label labelCardStateTitle;
    public Pane paneCardStateContent;
    public Label labelCardMemberCountTitle;
    public Pane paneCardMemberCountContent;
    public Label labelCardCoordinatorTitle;
    public Pane paneCardCoordinatorContent;
    public Label labelCardTopicCountTitle;
    public Pane paneCardTopicCountContent;
    public Label labelCardPartitionCountTitle;
    public Pane paneCardPartitionCountContent;
    public Label labelCardOverallLagTitle;
    public Pane paneCardOverallLagContent;
    public Label labelNotEditable;
    public Button buttonReset;
    public Button buttonDelete;
    public TabPane tabPane;
    public Tab tabMembers;
    public Tab tabTopics;
    public Tab tabAcl;

    private final ConsumerGroupService consumerGroupService;
    private final ConsumerGroupHelper consumerGroupHelper;
    private ConsumerGroup.GroupIdState groupIdState;

    @Inject
    public ConsumerGroupController(ConsumerGroupService consumerGroupService, ConsumerGroupHelper consumerGroupHelper) {
        this.consumerGroupService = consumerGroupService;
        this.consumerGroupHelper = consumerGroupHelper;
    }

    public void setGroupIdState(ConsumerGroup.GroupIdState groupIdState) {
        this.groupIdState = groupIdState;
    }

    @Override
    public void initFxml() {
        labelH1Comeback.setOnMouseClicked(it -> eventService.fire(EventService.DashboardEvent.LOAD_CONSUMER_GROUPS));

        labelCardTopicCountTitle.setTooltip(JavaFXUtils.tooltip(i18nService.get("consumerGroup.cardTopicCountDescription")));
        labelCardPartitionCountTitle.setTooltip(JavaFXUtils.tooltip(i18nService.get("consumerGroup.cardPartitionCountDescription")));

        themeService.setIcon(labelNotEditable, themeService.getIcon16("information_circle.png"));
        labelNotEditable.setTooltip(JavaFXUtils.tooltip(i18nService.get("consumerGroup.notEditable")));

        buttonReset.setOnAction(actionEvent ->
                viewManager.showEditConsumerGroupView(JavaFXUtils.getStage(actionEvent), groupIdState.groupId(), () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("consumerGroup.offsetsReset"));
                    loadSummary();
                    loadTab(tabPane.getSelectionModel().getSelectedItem());
                })
        );

        buttonDelete.setOnAction(sourceActionEvent ->
                viewManager.showDeleteConsumerGroupConfirmView(JavaFXUtils.getStage(sourceActionEvent), confirmCallback ->
                        futureTask(() -> consumerGroupService.delete(clusterId(), groupIdState.groupId()))
                                .onSuccess(it -> {
                                    confirmCallback.onSuccess();
                                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("consumerGroup.groupDeleted"));
                                    eventService.fire(EventService.DashboardEvent.LOAD_CONSUMER_GROUPS);
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
        labelH1.setText(groupIdState.groupId());
        if (consumerGroupHelper.isEditable(groupIdState.state())) enableEdit();
        else disableEdit();
        tabPane.getSelectionModel().select(0);
    }

    @Override
    public void initData() {
        Platform.runLater(() -> tabMembers.setContent(viewManager.loadConsumerGroupTabMembersView(groupIdState.groupId())));
        loadSummary();
    }

    private void enableEdit() {
        labelNotEditable.setVisible(false);
        buttonReset.setDisable(false);
        buttonDelete.setDisable(false);
    }

    private void disableEdit() {
        labelNotEditable.setVisible(true);
        buttonReset.setDisable(true);
        buttonDelete.setDisable(true);
    }

    private void loadTab(Tab tab) {
        if (disableLoadData) return;
        if (tabMembers.getId().equals(tab.getId())) {
            tabMembers.setContent(viewManager.loadConsumerGroupTabMembersView(groupIdState.groupId()));
            return;
        }
        if (tabTopics.getId().equals(tab.getId())) {
            tabTopics.setContent(viewManager.loadConsumerGroupTabTopicsView(groupIdState.groupId()));
            return;
        }
        if (tabAcl.getId().equals(tab.getId())) {
            tabAcl.setContent(viewManager.loadConsumerGroupTabAclView(groupIdState.groupId()));
            return;
        }
    }

    private void loadSummary() {
        JavaFXUtils.setPaneLoader(themeService.getIconLoader16(), paneCardStateContent, paneCardMemberCountContent, paneCardCoordinatorContent, paneCardTopicCountContent, paneCardPartitionCountContent, paneCardOverallLagContent);
        futureTask(() -> consumerGroupService.getGroupSummary(clusterId(), groupIdState.groupId()))
                .onSuccess(summary -> {
                    JavaFXUtils.setLabel(labelWithTooltip(prettyEnum(summary.state()), JavaFXUtils.tooltip(consumerGroupHelper.getStateDescription(summary.state()))), paneCardStateContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.memberCount()), paneCardMemberCountContent);
                    JavaFXUtils.setLabel(labelWithTooltip(summary.coordinator().getId(), JavaFXUtils.tooltip(summary.coordinator().getAddress())), paneCardCoordinatorContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.topicCount() + " (" + summary.assignedTopicCount() + ")"), paneCardTopicCountContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.partitionCount() + " (" + summary.assignedPartitionCount() + ")"), paneCardPartitionCountContent);
                    JavaFXUtils.setLabel(JavaFXUtils.label(summary.overallLag()), paneCardOverallLagContent);
                    if (consumerGroupHelper.isEditable(summary.state())) enableEdit();
                    else disableEdit();
                })
                .onError(it -> {
                    JavaFXUtils.setPaneNA(paneCardStateContent, paneCardMemberCountContent, paneCardCoordinatorContent, paneCardTopicCountContent, paneCardPartitionCountContent, paneCardOverallLagContent);
                    loadDataError(it);
                })
                .start();
    }
}
