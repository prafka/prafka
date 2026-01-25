package com.prafka.desktop.controller.onboarding;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.*;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;

/**
 * Controller for creating the master password during initial onboarding.
 *
 * <p>Guides new users through creating their master password, which encrypts cluster
 * credentials and sensitive data. Validates password strength and confirmation matching,
 * then completes the onboarding process and optionally prompts to add the first cluster.
 */
public class OnboardingCreateMasterPasswordController extends AbstractController {

    public PasswordField passwordFieldNewMasterPassword;
    public PasswordField passwordFieldRepeatNewMasterPassword;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonNext;

    private final MasterPasswordService masterPasswordService;
    private final OnboardingService onboardingService;
    private final ClusterService clusterService;
    private final StorageService storageService;

    @Inject
    public OnboardingCreateMasterPasswordController(MasterPasswordService masterPasswordService, OnboardingService onboardingService, ClusterService clusterService, StorageService storageService) {
        this.masterPasswordService = masterPasswordService;
        this.onboardingService = onboardingService;
        this.clusterService = clusterService;
        this.storageService = storageService;
    }

    @Override
    public void initFxml() {
        JavaFXUtils.requestFocus(passwordFieldNewMasterPassword);

        buttonNext.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            if (StringUtils.length(passwordFieldNewMasterPassword.getText()) < 6) {
                sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordInvalid"));
                return;
            }
            if (!Strings.CS.equals(passwordFieldNewMasterPassword.getText(), passwordFieldRepeatNewMasterPassword.getText())) {
                sceneService.addLabelError(paneAlert, i18nService.get("masterPassword.passwordsAreNotEqual"));
                return;
            }
            progressIndicator.setVisible(true);
            buttonNext.setDisable(true);
            task(() -> {
                masterPasswordService.saveMasterPassword(passwordFieldNewMasterPassword.getText());
                onboardingService.completeOnboarding();
                sessionService.setMasterPassword(passwordFieldNewMasterPassword.getText());
                storageService.loadEncryptedStorage();
            })
                    .onSuccess(it -> {
                        var dashboardStage = viewManager.showDashboardView();
                        if (clusterService.getClusters().isEmpty()) {
                            viewManager.showAddClusterView(dashboardStage, () -> {
                                sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterAdded"));
                                eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                            });
                        }
                        JavaFXUtils.getStage(actionEvent).close();
                    })
                    .onError(this::logError)
                    .start();
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    protected void onEnter() {
        buttonNext.fire();
    }
}
