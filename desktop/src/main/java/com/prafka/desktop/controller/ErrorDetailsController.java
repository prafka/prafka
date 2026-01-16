package com.prafka.desktop.controller;

import com.prafka.desktop.util.JavaFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class ErrorDetailsController extends AbstractController {

    public CodeArea codeArea;
    public Button buttonCancel;
    public Button buttonCopy;

    private Throwable throwableDetails;
    private String stringDetails;
    private String resultDetails;

    public void setData(Throwable throwableDetails) {
        this.throwableDetails = throwableDetails;
        var root = ExceptionUtils.getRootCause(throwableDetails);
        resultDetails = ExceptionUtils.getStackTrace(root == null ? throwableDetails : root);
    }

    public void setData(String stringDetails) {
        this.stringDetails = stringDetails;
        resultDetails = stringDetails;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonCopy.setOnAction(it -> {
            JavaFXUtils.copyToClipboard(resultDetails);
            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(it), Pos.BOTTOM_LEFT, i18nService.get("common.copiedToClipboard"));
        });
    }

    @Override
    public void initUi() {
        codeArea.replaceText(Strings.CS.removeEnd(resultDetails, "\n"));
    }
}
