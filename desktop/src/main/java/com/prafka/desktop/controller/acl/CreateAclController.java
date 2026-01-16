package com.prafka.desktop.controller.acl;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class CreateAclController extends AbstractController {

    public RadioButton radioButtonForConsumer;
    public RadioButton radioButtonForProducer;
    public RadioButton radioButtonForCustomNeed;
    public Pane paneTabContent;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonCreate;

    public Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        radioButtonForConsumer.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) paneTabContent.getChildren().setAll(viewManager.loadCreateAclForConsumerView(this));
        });
        radioButtonForProducer.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) paneTabContent.getChildren().setAll(viewManager.loadCreateAclForProducerView(this));
        });
        radioButtonForCustomNeed.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue)) paneTabContent.getChildren().setAll(viewManager.loadCreateAclForCustomNeedView(this));
        });
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    public void initUi() {
        paneTabContent.getChildren().setAll(viewManager.loadCreateAclForConsumerView(this));
    }

    @Override
    protected void onEnter() {
        buttonCreate.fire();
    }
}
