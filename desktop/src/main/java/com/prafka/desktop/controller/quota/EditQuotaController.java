package com.prafka.desktop.controller.quota;

import com.prafka.core.model.Quota;
import com.prafka.core.service.QuotaService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.Objects;

import static com.prafka.core.util.StreamUtils.tryOrEmpty;
import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

public class EditQuotaController extends AbstractController {

    public TextField textFieldEntityType;
    public TextField textFieldEntityName;
    public TextField textFieldConfigType;
    public TextField textFieldConfigValue;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonSave;

    private final QuotaService quotaService;

    private Quota quota;
    private Runnable onSuccess;

    @Inject
    public EditQuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    public void setData(Quota quota, Runnable onSuccess) {
        this.quota = quota;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            var configValue = tryOrEmpty(() -> Double.parseDouble(textFieldConfigValue.getText()));
            if (configValue.isEmpty()) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.value")));
                return;
            }
            progressIndicator.setVisible(true);
            buttonSave.setDisable(true);
            futureTask(() -> quotaService.update(clusterId(), quota.getEntity().getInternalType(), quota.getEntity().getName(), quota.getConfig().getInternalType(), configValue.get()))
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
        textFieldEntityType.setText(quota.getEntity().getType());
        textFieldEntityName.setText(quota.getEntity().getNameFormatted());
        textFieldConfigType.setText(quota.getConfig().getName());
        textFieldConfigValue.setText(Objects.toString(quota.getConfig().getValue(), null));
    }

    @Override
    protected void onEnter() {
        buttonSave.fire();
    }
}
