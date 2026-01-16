package com.prafka.desktop.controller.topic;

import com.prafka.core.service.ConfigService;
import com.prafka.core.service.TopicService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.ValidateUtils;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.TopicConfig;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class CreateTopicController extends AbstractController {

    public TextField textFieldName;
    public TextField textFieldPartitions;
    public TextField textFieldReplicationFactor;
    public ProgressIndicator progressIndicatorReplicationFactor;
    public ToggleGroup toggleGroupCleanupPolicy;
    public RadioButton radioButtonCleanupPolicyCompact;
    public RadioButton radioButtonCleanupPolicyDelete;
    public RadioButton radioButtonCleanupPolicyDeleteAndCompact;
    public GridPane paneAdvancedConfiguration;
    public TextArea textAreaAdditionalProperties;
    public HBox paneAlert;
    public ProgressIndicator progressIndicatorButtonBlock;
    public Button buttonCancel;
    public Button buttonCreateTopic;

    private final TopicService topicService;
    private final ConfigService configService;
    private final Map<String, TextField> advancedConfigurationTextFieldMap = new HashMap<>();
    private Runnable onSuccess;

    @Inject
    public CreateTopicController(TopicService topicService, ConfigService configService) {
        this.topicService = topicService;
        this.configService = configService;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        textFieldPartitions.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(3L));

        textFieldReplicationFactor.setTextFormatter(JavaFXUtils.positiveLongTextFormatter(1L));

        var defaultConfigs = configService.getTopicDefaultConfigs().stream().sorted(Comparator.comparing(ConfigService.DefaultConfig::name)).toList();
        for (int i = 0; i < defaultConfigs.size(); i++) {
            var config = defaultConfigs.get(i);
            var label = JavaFXUtils.label(config.name(), "font-code");
            label.setTooltip(JavaFXUtils.tooltip(config.documentation()));
            var textField = new TextField();
            textField.setPromptText(String.valueOf(config.value()));
            GridPane.setMargin(textField, new Insets(2.5, 0, 2.5, 0));
            paneAdvancedConfiguration.add(label, 0, i);
            paneAdvancedConfiguration.add(textField, 1, i);
            advancedConfigurationTextFieldMap.put(config.name(), textField);
        }

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());

        buttonCreateTopic.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var name = textFieldName.getText();
            if (StringUtils.isBlank(name)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.name")));
                return;
            }

            int partitions;
            try {
                partitions = Integer.parseInt(textFieldPartitions.getText());
            } catch (Exception e) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.partitions")));
                return;
            }

            short replicationFactor;
            try {
                replicationFactor = Short.parseShort(textFieldReplicationFactor.getText());
            } catch (Exception e) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.replicationFactor")));
                return;
            }

            var configs = new HashMap<String, String>();

            String cleanupPolicy = "";
            var radioButtonCleanupPolicy = (RadioButton) toggleGroupCleanupPolicy.getSelectedToggle();
            if (radioButtonCleanupPolicy.getId().equals(radioButtonCleanupPolicyDelete.getId())) {
                cleanupPolicy = TopicConfig.CLEANUP_POLICY_DELETE;
            } else if (radioButtonCleanupPolicy.getId().equals(radioButtonCleanupPolicyCompact.getId())) {
                cleanupPolicy = TopicConfig.CLEANUP_POLICY_COMPACT;
            } else if (radioButtonCleanupPolicy.getId().equals(radioButtonCleanupPolicyDeleteAndCompact.getId())) {
                cleanupPolicy = TopicConfig.CLEANUP_POLICY_DELETE + "," + TopicConfig.CLEANUP_POLICY_COMPACT;
            }
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);

            advancedConfigurationTextFieldMap.forEach((key, textField) -> {
                if (StringUtils.isNotBlank(textField.getText())) {
                    configs.put(key, textField.getText());
                }
            });

            try {
                ValidateUtils.validateAdditionalProperties(textAreaAdditionalProperties);
                configs.putAll(ValidateUtils.getAdditionalProperties(textAreaAdditionalProperties));
            } catch (IllegalArgumentException e) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.additionalProperties")));
                return;
            }

            progressIndicatorButtonBlock.setVisible(true);
            buttonCreateTopic.setDisable(true);
            futureTask(() -> topicService.create(clusterId(), name, partitions, replicationFactor, configs))
                    .onSuccess(topic -> {
                        JavaFXUtils.getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonCreateTopic.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        textFieldReplicationFactor.setDisable(true);
        progressIndicatorReplicationFactor.setVisible(true);
        futureTask(() -> configService.getDefaultReplicationFactor(clusterId()))
                .onSuccess(config -> {
                    progressIndicatorReplicationFactor.setVisible(false);
                    textFieldReplicationFactor.setDisable(false);
                    textFieldReplicationFactor.setText(config.getValue());
                })
                .onError(it -> {
                    progressIndicatorReplicationFactor.setVisible(false);
                    textFieldReplicationFactor.setDisable(false);
                    logError(it);
                })
                .start();
    }

    @Override
    protected void onEnter() {
        buttonCreateTopic.fire();
    }
}
