package com.prafka.desktop.controller.topic;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.scene.control.Button;

/**
 * Controller for displaying JavaScript filter documentation.
 *
 * <p>Shows help information about available variables and functions
 * that can be used in JavaScript filter expressions.
 */
public class JsFilterDocumentationController extends AbstractController {

    public Button buttonOk;

    @Override
    public void initFxml() {
        buttonOk.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }
}
