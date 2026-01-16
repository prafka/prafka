package com.prafka.desktop.controller.connect;

import com.prafka.core.model.Connector;
import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import static com.prafka.core.util.JsonFactory.*;
import static com.prafka.core.util.StreamUtils.tryOrEmpty;

public class EditConnectorController extends AbstractController {

    public CodeArea codeArea;
    public ProgressIndicator progressIndicator;
    public HBox paneAlert;
    public ProgressIndicator progressIndicatorButtonBlock;
    public Button buttonCancel;
    public Button buttonSave;

    private final ConnectService connectService;
    private Subscription codeHighlightSubscription;
    private Connector.Name cn;
    private Runnable onSuccess;

    @Inject
    public EditConnectorController(ConnectService connectService) {
        this.connectService = connectService;
    }

    public void setData(Connector.Name cn, Runnable onSuccess) {
        this.cn = cn;
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        JavaFXUtils.setCodeAreaAutoIntend(codeArea);
        codeHighlightSubscription = CodeHighlight.codeHighlightSubscription(codeArea, () -> CodeHighlight.highlightJson(codeArea.getText()));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    public void initUi() {
        progressIndicator.setVisible(true);
        codeArea.setEditable(false);
        buttonSave.setDisable(true);
        FutureServiceAdapter.futureTask(() -> connectService.get(clusterId(), cn))
                .onSuccess(connector -> {
                    progressIndicator.setVisible(false);
                    codeArea.setEditable(true);
                    buttonSave.setDisable(false);
                    doInit(connector);
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    loadDataError(Pos.BOTTOM_LEFT, it);
                })
                .start();
    }

    private void doInit(Connector connector) {
        codeArea.replaceText(gsonPretty.toJson(connector.getConfig()));
        ServiceAdapter.task(() -> CodeHighlight.highlightJson(codeArea.getText()))
                .onSuccess(it -> codeArea.setStyleSpans(0, it))
                .start();

        buttonSave.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var config = tryOrEmpty(() -> gsonDefault.fromJson(codeArea.getText(), MAP_STING_STRING_TYPE));
            if (config.isEmpty() || config.get().isEmpty()) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.configuration")));
                return;
            }

            progressIndicatorButtonBlock.setVisible(true);
            buttonSave.setDisable(true);
            FutureServiceAdapter.futureTask(() -> connectService.update(clusterId(), cn, config.get()))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        progressIndicatorButtonBlock.setVisible(false);
                        buttonSave.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void close() {
        super.close();
        if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
    }
}
