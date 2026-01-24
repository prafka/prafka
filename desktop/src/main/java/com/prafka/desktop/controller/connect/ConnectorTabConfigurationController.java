package com.prafka.desktop.controller.connect;

import com.prafka.core.model.Connector;
import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import static com.prafka.core.util.JsonFactory.gsonPretty;

/**
 * Controller for the connector configuration tab showing the current config.
 *
 * <p>Displays the connector configuration as formatted JSON with syntax highlighting.
 * Provides an edit button to open the configuration editor dialog.
 */
public class ConnectorTabConfigurationController extends AbstractController {

    public Button buttonEdit;
    public CodeArea codeArea;
    public ProgressIndicator progressIndicator;

    private final ConnectService connectService;
    private Connector.Name cn;

    @Inject
    public ConnectorTabConfigurationController(ConnectService connectService) {
        this.connectService = connectService;
    }

    public void setCn(Connector.Name cn) {
        this.cn = cn;
    }

    @Override
    public void initFxml() {
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    }

    @Override
    public void initData() {
        loadData();
    }

    private void loadData() {
        progressIndicator.setVisible(true);
        FutureServiceAdapter.futureTask(() -> connectService.get(clusterId(), cn))
                .onSuccess(connector -> {
                    progressIndicator.setVisible(false);
                    buttonEdit.setOnAction(it -> viewManager.showEditConnectorView(JavaFXUtils.getStage(it), cn, this::loadData));
                    codeArea.replaceText(gsonPretty.toJson(connector.getConfig()));
                    ServiceAdapter.task(() -> CodeHighlight.highlightJson(codeArea.getText()))
                            .onSuccess(it -> codeArea.setStyleSpans(0, it))
                            .start();
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    loadDataError(it);
                })
                .start();
    }
}
