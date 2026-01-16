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
import org.apache.commons.lang3.Strings;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class ChangeMasterPasswordController extends AbstractController {

    public PasswordField passwordFieldCurrentMasterPassword;
    public PasswordField passwordFieldNewMasterPassword;
    public PasswordField passwordFieldRepeatNewMasterPassword;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonBack;
    public Button buttonChange;

    private final MasterPasswordService masterPasswordService;
    private final StorageService storageService;

    @Inject
    public ChangeMasterPasswordController(MasterPasswordService masterPasswordService, StorageService storageService) {
        this.masterPasswordService = masterPasswordService;
        this.storageService = storageService;
    }

    @Override
    public void initFxml() {
        JavaFXUtils.requestFocus(passwordFieldCurrentMasterPassword);

        buttonChange.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            if (StringUtils.isBlank(passwordFieldCurrentMasterPassword.getText())) {
                sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordEmpty"));
                return;
            }
            progressIndicator.setVisible(true);
            buttonChange.setDisable(true);
            task(() -> masterPasswordService.checkMasterPassword(passwordFieldCurrentMasterPassword.getText()))
                    .onSuccess(result -> {
                        progressIndicator.setVisible(false);
                        buttonChange.setDisable(false);
                        if (!result) {
                            sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordWrong"));
                            return;
                        }
                        if (StringUtils.length(passwordFieldNewMasterPassword.getText()) < 6) {
                            sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordInvalid"));
                            return;
                        }
                        if (!Strings.CS.equals(passwordFieldNewMasterPassword.getText(), passwordFieldRepeatNewMasterPassword.getText())) {
                            sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordsAreNotEqual"));
                            return;
                        }
                        progressIndicator.setVisible(true);
                        buttonChange.setDisable(true);
                        task(() -> {
                            sessionService.setMasterPassword(passwordFieldCurrentMasterPassword.getText());
                            storageService.loadEncryptedStorage();
                            masterPasswordService.saveMasterPassword(passwordFieldNewMasterPassword.getText());
                            sessionService.setMasterPassword(passwordFieldNewMasterPassword.getText());
                            storageService.deleteEncryptedStorage();
                            storageService.loadEncryptedStorage();
                        })
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

        buttonBack.setOnAction(it -> viewManager.showEnterMasterPasswordView(JavaFXUtils.getStage(it)));
    }

    @Override
    protected void onEnter() {
        buttonChange.fire();
    }
}
