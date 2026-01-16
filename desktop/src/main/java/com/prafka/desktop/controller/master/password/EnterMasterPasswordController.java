package com.prafka.desktop.controller.master.password;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.MasterPasswordService;
import com.prafka.desktop.service.StorageService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class EnterMasterPasswordController extends AbstractController {

    public PasswordField passwordFieldCurrentMasterPassword;
    public Button buttonChangeMasterPassword;
    public Button buttonResetMasterPassword;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonSubmit;

    private final MasterPasswordService masterPasswordService;
    private final StorageService storageService;

    @Inject
    public EnterMasterPasswordController(MasterPasswordService masterPasswordService, StorageService storageService) {
        this.masterPasswordService = masterPasswordService;
        this.storageService = storageService;
    }

    @Override
    public void initFxml() {
        JavaFXUtils.requestFocus(passwordFieldCurrentMasterPassword);

        buttonChangeMasterPassword.setOnAction(it -> viewManager.showChangeMasterPasswordView(JavaFXUtils.getStage(it)));
        buttonResetMasterPassword.setOnAction(it -> viewManager.showResetMasterPasswordView(JavaFXUtils.getStage(it)));

        buttonSubmit.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            if (StringUtils.isBlank(passwordFieldCurrentMasterPassword.getText())) {
                sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordEmpty"));
                return;
            }
            progressIndicator.setVisible(true);
            buttonSubmit.setDisable(true);
            task(() -> masterPasswordService.checkMasterPassword(passwordFieldCurrentMasterPassword.getText()))
                    .onSuccess(result -> {
                        if (!result) {
                            progressIndicator.setVisible(false);
                            buttonSubmit.setDisable(false);
                            sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordWrong"));
                            return;
                        }
                        sessionService.setMasterPassword(passwordFieldCurrentMasterPassword.getText());
                        task(storageService::loadEncryptedStorage)
                                .onSuccess(it -> {
                                    viewManager.showDashboardView();
                                    JavaFXUtils.getStage(actionEvent).close();
                                })
                                .onError(this::logError)
                                .start();
                    })
                    .onError(this::logError)
                    .start();
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    protected void onEnter() {
        buttonSubmit.fire();
    }
}
