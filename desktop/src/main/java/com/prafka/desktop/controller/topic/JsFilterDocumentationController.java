package com.prafka.desktop.controller.topic;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.control.Button;

public class JsFilterDocumentationController extends AbstractController {

    public Button buttonOk;

    @Override
    public void initFxml() {
        buttonOk.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }
}
