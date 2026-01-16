package com.prafka.desktop.controller.broker;

import com.prafka.core.model.Broker;
import com.prafka.core.model.Config;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class EditBrokerConfigController extends AbstractController {

    public Label labelInfoTitle;
    public Label labelInfoContent;
    public TextField textFieldDefaultValue;
    public TextField textFieldCurrentValue;
    public Button buttonCancel;

    private Broker broker;
    private Config config;

    public void setData(Broker broker, Config config) {
        this.broker = broker;
        this.config = config;
    }

    @Override
    public void initFxml() {
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    public void initUi() {
        labelInfoTitle.setText(config.getName());
        labelInfoContent.setText(config.getDocumentation());
        textFieldDefaultValue.setText(config.getDefaultValue());
        textFieldCurrentValue.setText(config.getValue());
    }
}
