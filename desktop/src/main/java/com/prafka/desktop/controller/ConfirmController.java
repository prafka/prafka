package com.prafka.desktop.controller;

import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

/**
 * Controller for confirmation dialog windows.
 *
 * <p>Displays a confirmation prompt with customizable title, content, and button text,
 * handling success and error callbacks for the confirm action.
 */
public class ConfirmController extends AbstractController {

    public Label labelTitle;
    public Label labelContent;
    public Pane paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonConfirm;

    public void setData(String title, String content, String button, Consumer<ConfirmCallback> onButton) {
        labelTitle.setText(title);
        labelContent.setText(content);

        buttonConfirm.setText(button);
        buttonConfirm.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();
            progressIndicator.setVisible(true);
            buttonConfirm.setDisable(true);
            onButton.accept(new ConfirmCallback() {
                @Override
                public void onSuccess() {
                    JavaFXUtils.getStage(actionEvent).close();
                }

                @Override
                public void onError(Throwable throwable) {
                    progressIndicator.setVisible(false);
                    buttonConfirm.setDisable(false);
                    sceneService.addPaneAlertError(paneAlert, throwable);
                    logError(throwable);
                }
            });
        });

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    public interface ConfirmCallback {
        void onSuccess();

        void onError(Throwable throwable);
    }

    @Override
    public void initScene(Scene scene) {
        buttonConfirm.requestFocus();
    }

    @Override
    protected void onEnter() {
        buttonConfirm.fire();
    }
}
