package com.prafka.desktop.controller.quota;

import com.prafka.core.model.Quota;
import com.prafka.core.service.QuotaService;
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
import org.apache.commons.lang3.StringUtils;

import static com.prafka.core.util.StreamUtils.tryOrEmpty;
import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

/**
 * Controller for creating new Kafka quotas.
 *
 * <p>Provides a form to configure quota entity (user, client ID, or IP), entity name,
 * quota type (producer/consumer rate, connection rate, etc.), and quota value.
 * Validates input and creates the quota configuration in the cluster.
 */
public class CreateQuotaController extends AbstractController {

    public Pane paneContent;
    public ComboBox<Quota.EntityType> comboBoxEntityType;
    public TextField textFieldEntityName;
    public ComboBox<Quota.ConfigType> comboBoxConfigType;
    public TextField textFieldConfigValue;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonCreate;

    private final QuotaService quotaService;
    private Runnable onSuccess;

    @Inject
    public CreateQuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        comboBoxEntityType.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxEntityType.getItems().setAll(
                Quota.EntityType.USER,
                Quota.EntityType.CLIENT_ID,
                Quota.EntityType.IP
        );
        comboBoxEntityType.setConverter(new StringConverter<>() {
            @Override
            public String toString(Quota.EntityType entityType) {
                return entityType == null ? null : entityType.getValue();
            }

            @Override
            public Quota.EntityType fromString(String string) {
                return null;
            }
        });
        comboBoxEntityType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) return;
            comboBoxConfigType.getItems().clear();
            comboBoxConfigType.getSelectionModel().clearSelection();
            switch (newValue) {
                case USER, CLIENT_ID -> comboBoxConfigType.getItems().setAll(
                        Quota.ConfigType.PRODUCER_RATE,
                        Quota.ConfigType.CONSUMER_RATE,
                        Quota.ConfigType.CONTROLLER_RATE,
                        Quota.ConfigType.REQUEST_PERCENTAGE
                );
                case IP -> comboBoxConfigType.getItems().setAll(Quota.ConfigType.CONNECTION_RATE);
            }
        });

        comboBoxConfigType.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxConfigType.setConverter(new StringConverter<>() {
            @Override
            public String toString(Quota.ConfigType configType) {
                return configType == null ? null : configType.getValue();
            }

            @Override
            public Quota.ConfigType fromString(String string) {
                return null;
            }
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonCreate.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var entityType = comboBoxEntityType.getValue();
            if (entityType == null) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("quota.entityType")));
                return;
            }

            var entityName = StringUtils.isBlank(textFieldEntityName.getText()) ? null : textFieldEntityName.getText();

            var configType = comboBoxConfigType.getValue();
            if (configType == null) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.property")));
                return;
            }

            var configValue = tryOrEmpty(() -> Double.parseDouble(textFieldConfigValue.getText()));
            if (configValue.isEmpty()) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.value")));
                return;
            }

            progressIndicator.setVisible(true);
            buttonCreate.setDisable(true);
            futureTask(() -> quotaService.create(clusterId(), entityType, entityName, configType, configValue.get()))
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
    protected void onEnter() {
        buttonCreate.fire();
    }
}
