package com.prafka.desktop.controller;

import com.prafka.core.model.Config;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class RawConfigController extends AbstractController {

    public CodeArea codeArea;
    public Button buttonCancel;
    public Button buttonCopy;

    private String configList;

    public void setConfigList(List<Config> configList) {
        this.configList = configList.stream()
                .sorted(Comparator.comparing(Config::getName))
                .map(it -> it.getName() + "=" + it.getValue())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonCopy.setOnAction(it -> {
            JavaFXUtils.copyToClipboard(configList);
            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(it), Pos.BOTTOM_LEFT, i18nService.get("common.copiedToClipboard"));
        });
    }

    @Override
    public void initUi() {
        codeArea.replaceText(configList);
        task(() -> CodeHighlight.highlightKV(configList))
                .onSuccess(it -> codeArea.setStyleSpans(0, it))
                .start();
    }
}
