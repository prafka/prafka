package com.prafka.desktop.controller.topic;

import com.prafka.core.model.ConsumeFilter;
import com.prafka.core.service.RecordService;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class AddEditJsFilterController extends AbstractController {

    public CodeArea codeArea;
    public TextField textFieldName;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonDocumentation;
    public Button buttonApply;

    private final RecordService recordService;
    private Subscription codeHighlightSubscription;
    private Optional<ConsumeFilter.Expression> jsFilter;
    private Consumer<ConsumeFilter.Expression> onSuccess;

    @Inject
    public AddEditJsFilterController(RecordService recordService) {
        this.recordService = recordService;
    }

    public void setData(Consumer<ConsumeFilter.Expression> onSuccess) {
        this.jsFilter = Optional.empty();
        this.onSuccess = onSuccess;
    }

    public void setData(ConsumeFilter.Expression jsFilter, Consumer<ConsumeFilter.Expression> onSuccess) {
        this.jsFilter = Optional.of(jsFilter);
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        JavaFXUtils.setCodeAreaAutoIntend(codeArea);
        codeArea.getStyleClass().addAll("code-area-js", "code-area-border");
        codeHighlightSubscription = CodeHighlight.codeHighlightSubscription(codeArea, () -> CodeHighlight.highlightJs(codeArea.getText()));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());

        buttonDocumentation.setOnAction(it -> viewManager.showJsFilterDocumentationView(JavaFXUtils.getStage(it)));

        buttonApply.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            if (StringUtils.isBlank(codeArea.getText()) || !Strings.CS.contains(codeArea.getText(), "return ")) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.code")));
                return;
            }

            var expression = new ConsumeFilter.Expression(textFieldName.getText(), codeArea.getText(), true);

            progressIndicator.setVisible(true);
            buttonApply.setDisable(true);
            futureTask(() -> recordService.tryCompileExpression(expression.code()))
                    .onSuccess(it -> {
                        onSuccess.accept(expression);
                        JavaFXUtils.getStage(actionEvent).close();
                    })
                    .onError(throwable -> {
                        progressIndicator.setVisible(false);
                        buttonApply.setDisable(false);
                        sceneService.addPaneAlertError(paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        jsFilter.ifPresent(expr -> {
            textFieldName.setText(expr.name());
            codeArea.replaceText(expr.code());
            task(() -> CodeHighlight.highlightJs(expr.code()))
                    .onSuccess(it -> codeArea.setStyleSpans(0, it))
                    .start();
        });
    }

    @Override
    protected void onEnter() {
        buttonApply.fire();
    }

    @Override
    public void close() {
        super.close();
        if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
    }
}
