package com.prafka.desktop.controller.topic;

import com.prafka.core.model.Config;
import com.prafka.core.service.ConfigService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class EditTopicConfigController extends AbstractController {

    public Label labelInfoTitle;
    public Label labelInfoContent;
    public TextField textFieldDefaultValue;
    public TextField textFieldCurrentValue;
    public TextField textFieldNewValue;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonDefault;
    public Button buttonSave;

    private final ConfigService configService;
    private String topicName;
    private Config config;
    private Runnable onSuccess;

    @Inject
    public EditTopicConfigController(ConfigService configService) {
        this.configService = configService;
    }

    public void setData(String topicName, Config config, Runnable onSuccess) {
        this.topicName = topicName;
        this.config = config;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());

        buttonDefault.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicator.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> configService.resetByTopic(clusterId(), topicName, config.getName()))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
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

        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            var configValue = textFieldNewValue.getText();
            if (StringUtils.isBlank(configValue)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.newValue")));
                return;
            }
            progressIndicator.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> configService.setByTopic(clusterId(), topicName, config.getName(), configValue))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
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
        labelInfoTitle.setText(config.getName());
        labelInfoContent.setText(config.getDocumentation());
        textFieldDefaultValue.setText(config.getDefaultValue());
        textFieldCurrentValue.setText(config.getValue());
    }

    @Override
    protected void onEnter() {
        buttonSave.fire();
    }
}
