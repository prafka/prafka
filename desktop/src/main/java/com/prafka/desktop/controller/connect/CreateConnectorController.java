package com.prafka.desktop.controller.connect;

import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import java.util.Comparator;
import java.util.HashMap;

import static com.prafka.core.util.JsonFactory.*;
import static com.prafka.core.util.StreamUtils.tryOrEmpty;

public class CreateConnectorController extends AbstractController {

    public Pane paneContent;
    public TextField textFieldName;
    public ComboBox<ClusterModel.ConnectModel> comboBoxConnect;
    public CodeArea codeArea;
    public HBox paneAlert;
    public ProgressIndicator progressIndicator;
    public Button buttonCancel;
    public Button buttonValidate;
    public Button buttonCreate;

    private final ConnectService connectService;
    private Subscription codeHighlightSubscription;
    private Runnable onSuccess;

    @Inject
    public CreateConnectorController(ConnectService connectService) {
        this.connectService = connectService;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @Override
    public void initFxml() {
        comboBoxConnect.prefWidthProperty().bind(paneContent.widthProperty());
        comboBoxConnect.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClusterModel.ConnectModel connect) {
                return connect.getName() + " (" + connect.getUrl() + ")";
            }

            @Override
            public ClusterModel.ConnectModel fromString(String string) {
                return null;
            }
        });

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().addAll("code-area-json", "code-area-border");
        JavaFXUtils.setCodeAreaAutoIntend(codeArea);
        codeArea.replaceText(TEMPLATE);
        codeHighlightSubscription = CodeHighlight.codeHighlightSubscription(codeArea, () -> CodeHighlight.highlightJson(codeArea.getText()));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());

        buttonValidate.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var name = textFieldName.getText();
            if (StringUtils.isBlank(name)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.name")));
                return;
            }

            var connect = comboBoxConnect.getValue();
            if (connect == null) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("connect.connect")));
                return;
            }

            var config = tryOrEmpty(() -> gsonDefault.fromJson(codeArea.getText(), MAP_STING_STRING_TYPE));
            if (config.isEmpty() || config.get().isEmpty()) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.configuration")));
                return;
            }
            config.get().put("name", name);

            var plugin = ClassUtils.getShortClassName(config.get().get("connector.class"));
            if (StringUtils.isBlank(plugin)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.configuration")));
                return;
            }

            progressIndicator.setVisible(true);
            buttonValidate.setDisable(true);
            buttonCreate.setDisable(true);
            FutureServiceAdapter.futureTask(() -> connectService.validate(clusterId(), connect.getId(), plugin, config.get()))
                    .onSuccess(it -> {
                        progressIndicator.setVisible(false);
                        buttonValidate.setDisable(false);
                        buttonCreate.setDisable(false);
                        sceneService.addLabelSuccess(paneAlert, i18nService.get("connect.validateSuccess"));
                    })
                    .onError(throwable -> {
                        sceneService.addLabelError(paneAlert, i18nService.get("connect.validateFailed"));
                        progressIndicator.setVisible(false);
                        buttonValidate.setDisable(false);
                        buttonCreate.setDisable(false);
                        var root = ExceptionUtils.getRootCause(throwable);
                        if (root instanceof ConnectService.ConnectorValidateError ex) {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, gsonPretty.toJson(ex.getConfigs()));
                        } else {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, throwable);
                        }
                        logError(throwable);
                    })
                    .start();
        });

        buttonCreate.setOnAction(actionEvent -> {
            paneAlert.getChildren().clear();

            var name = textFieldName.getText();
            if (StringUtils.isBlank(name)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.name")));
                return;
            }

            var connect = comboBoxConnect.getValue();
            if (connect == null) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("connect.connect")));
                return;
            }

            var config = tryOrEmpty(() -> gsonDefault.fromJson(codeArea.getText(), MAP_STING_STRING_TYPE));
            if (config.isEmpty() || config.get().isEmpty()) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.configuration")));
                return;
            }
            var configForValidate = new HashMap<>(config.get());
            configForValidate.put("name", name);

            var plugin = ClassUtils.getShortClassName(config.get().get("connector.class"));
            if (StringUtils.isBlank(plugin)) {
                sceneService.addLabelError(paneAlert, i18nService.get("common.checkParameter").formatted(i18nService.get("common.configuration")));
                return;
            }

            progressIndicator.setVisible(true);
            buttonValidate.setDisable(true);
            buttonCreate.setDisable(true);
            FutureServiceAdapter.futureTask(() -> connectService.validate(clusterId(), connect.getId(), plugin, configForValidate).thenCompose(it -> connectService.create(clusterId(), connect.getId(), name, config.get())))
                    .onSuccess(it -> {
                        JavaFXUtils.getStage(actionEvent).close();
                        onSuccess.run();
                    })
                    .onError(throwable -> {
                        sceneService.addLabelError(paneAlert, i18nService.get("common.error"));
                        progressIndicator.setVisible(false);
                        buttonValidate.setDisable(false);
                        buttonCreate.setDisable(false);
                        var root = ExceptionUtils.getRootCause(throwable);
                        if (root instanceof ConnectService.ConnectorValidateError ex) {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, gsonPretty.toJson(ex.getConfigs()));
                        } else {
                            sceneService.addHyperlinkErrorDetailed(paneAlert, throwable);
                        }
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        comboBoxConnect.getItems().addAll(sessionService.getCluster().getConnects().stream().sorted(Comparator.comparing(ClusterModel.ConnectModel::getCreatedAt).reversed()).toList());
    }

    @Override
    protected void onEnter() {
        buttonCreate.fire();
    }

    @Override
    public void close() {
        super.close();
        if (codeHighlightSubscription != null) codeHighlightSubscription.unsubscribe();
    }

    private static final String TEMPLATE = """
            {
            
            }
            """;
}
