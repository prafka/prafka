package com.prafka.desktop.controller.acl;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Controller for the create ACL dialog with mode selection.
 *
 * <p>Provides a wizard-style interface to create Kafka ACLs with three modes:
 * consumer ACLs (read access), producer ACLs (write access), or custom ACLs
 * (fine-grained permission control). Routes to the appropriate sub-controller.
 */
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
