package com.prafka.desktop.controller.cluster;

import com.prafka.core.connection.AuthenticationMethod;
import com.prafka.core.manager.KafkaManager;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ConnectionPropertiesService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.ValidateUtils;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableSupplier;

import java.util.Optional;

/**
 * Controller for the Schema Registry configuration tab in the add/edit cluster dialog.
 *
 * <p>Manages Schema Registry URL and authentication settings (Basic, Token, SSL)
 * with connection testing capability.
 */
public class AddEditClusterTabSchemaRegistryController extends AbstractAddEditClusterTabController {

    public TextField textFieldUrl;
    public RadioButton radioButtonNoneAuthentication;
    public RadioButton radioButtonBasicAuthentication;
    public RadioButton radioButtonTokenAuthentication;
    public RadioButton radioButtonSslAuthentication;
    public Pane paneAuthentication;
    public TextField textFieldBasicUsername;
    public PasswordField passwordFieldBasicPassword;
    public TextField textFieldToken;
    public TextArea textAreaAdditionalProperties;
    public Button buttonTestConnection;

    private final KafkaManager kafkaManager;
    private final ConnectionPropertiesService connectionPropertiesService;
    private AddEditClusterController parentController;
    private Optional<ClusterModel> cluster;

    @Inject
    public AddEditClusterTabSchemaRegistryController(KafkaManager kafkaManager, ConnectionPropertiesService connectionPropertiesService) {
        this.kafkaManager = kafkaManager;
        this.connectionPropertiesService = connectionPropertiesService;
    }

    public void setData(AddEditClusterController parentController, Optional<ClusterModel> cluster) {
        this.parentController = parentController;
        this.cluster = cluster;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        textFieldBasicUsername = new TextField();
        passwordFieldBasicPassword = new PasswordField();

        textFieldToken = new TextField();

        radioButtonNoneAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneAuthentication.getChildren().clear();
        });
        radioButtonBasicAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneBasicAuthentication();
        });
        radioButtonTokenAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneTokenAuthentication();
        });
        radioButtonSslAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneSslAuthentication(paneAuthentication);
        });

        buttonTestConnection.setOnAction(actionEvent -> {
            parentController.buttonSave.requestFocus();
            parentController.paneAlert.getChildren().clear();
            try {
                validate();
            } catch (IllegalArgumentException e) {
                sceneService.addLabelError(parentController.paneAlert, e.getMessage());
                return;
            }
            parentController.progressIndicator.setVisible(true);
            buttonTestConnection.setDisable(true);
            ServiceAdapter.task(getTaskForTestConnection())
                    .onSuccess(it -> {
                        parentController.progressIndicator.setVisible(false);
                        buttonTestConnection.setDisable(false);
                        sceneService.addLabelSuccess(parentController.paneAlert, i18nService.get("createClusterView.labelSuccessTestConnection"));
                    })
                    .onError(throwable -> {
                        parentController.progressIndicator.setVisible(false);
                        buttonTestConnection.setDisable(false);
                        sceneService.addLabelError(parentController.paneAlert, i18nService.get("createClusterView.labelFailedTestConnection"));
                        sceneService.addHyperlinkErrorDetailed(parentController.paneAlert, throwable);
                        logError(throwable);
                    })
                    .start();
        });
    }

    @Override
    public void initUi() {
        if (cluster.isPresent() && cluster.get().isSchemaRegistryDefined()) {
            var schemaRegistry = cluster.get().getSchemaRegistry();
            textFieldUrl.setText(schemaRegistry.getUrl());
            switch (schemaRegistry.getAuthenticationMethod()) {
                case NONE -> radioButtonNoneAuthentication.setSelected(true);
                case BASIC -> {
                    radioButtonBasicAuthentication.setSelected(true);
                    textFieldBasicUsername.setText(schemaRegistry.getBasicAuthentication().getUsername());
                    passwordFieldBasicPassword.setText(stringOrNull(schemaRegistry.getBasicAuthentication().getPassword()));
                }
                case TOKEN -> {
                    radioButtonTokenAuthentication.setSelected(true);
                    textFieldToken.setText(stringOrNull(schemaRegistry.getTokenAuthentication().getToken()));
                }
                case SSL -> {
                    radioButtonSslAuthentication.setSelected(true);
                    fillSslFields(schemaRegistry.getSslAuthentication());
                }
            }
            textAreaAdditionalProperties.setText(ValidateUtils.getAdditionalProperties(schemaRegistry.getAdditionalProperties()));
        }
    }

    private void fillPaneBasicAuthentication() {
        var pane = new GridPane();
        pane.getColumnConstraints().addAll(new ColumnConstraints() {{
            setPercentWidth(50);
        }}, new ColumnConstraints() {{
            setPercentWidth(50);
        }});

        var username = new VBox(5, JavaFXUtils.label(i18nService.get("common.username"), "font-medium"), textFieldBasicUsername);
        GridPane.setMargin(username, new Insets(0, 10, 0, 0));

        var password = new VBox(5, JavaFXUtils.label(i18nService.get("common.password"), "font-medium"), passwordFieldBasicPassword);
        GridPane.setMargin(password, new Insets(0, 0, 0, 10));

        pane.add(username, 0, 0);
        pane.add(password, 1, 0);

        VBox.setMargin(pane, new Insets(20, 0, 0, 0));

        paneAuthentication.getChildren().setAll(pane);
    }

    private void fillPaneTokenAuthentication() {
        var pane = new VBox(5, JavaFXUtils.label(i18nService.get("common.bearerToken"), "font-medium"), textFieldToken);
        VBox.setMargin(pane, new Insets(20, 0, 0, 0));
        paneAuthentication.getChildren().setAll(pane);
    }

    FailableSupplier<String, Exception> getTaskForTestConnection() {
        return () -> {
            var cluster = new ClusterModel();
            fillClusterModel(cluster);
            return kafkaManager.createSchemaRegistryRestService(connectionPropertiesService.getSchemaRegistryProperties(cluster.getSchemaRegistry())).getClusterId().getId();
        };
    }

    void fillClusterModel(ClusterModel cluster) {
        if (ValidateUtils.isNotUrl(textFieldUrl.getText())) {
            cluster.setSchemaRegistry(null);
            return;
        }
        var schemaRegistry = cluster.getSchemaRegistry();
        if (schemaRegistry == null) {
            schemaRegistry = new ClusterModel.SchemaRegistryModel();
            cluster.setSchemaRegistry(schemaRegistry);
        }
        schemaRegistry.setUrl(textFieldUrl.getText());
        if (radioButtonNoneAuthentication.isSelected()) {
            schemaRegistry.setAuthenticationMethod(AuthenticationMethod.NONE);
            schemaRegistry.setBasicAuthentication(null);
            schemaRegistry.setTokenAuthentication(null);
            schemaRegistry.setSslAuthentication(null);
        }
        if (radioButtonBasicAuthentication.isSelected()) {
            schemaRegistry.setAuthenticationMethod(AuthenticationMethod.BASIC);
            var basic = new ClusterModel.BasicAuthentication();
            basic.setUsername(textFieldBasicUsername.getText());
            basic.setPassword(charArrayOrNull(passwordFieldBasicPassword.getText()));
            schemaRegistry.setBasicAuthentication(basic);
            schemaRegistry.setTokenAuthentication(null);
            schemaRegistry.setSslAuthentication(null);
        }
        if (radioButtonTokenAuthentication.isSelected()) {
            schemaRegistry.setAuthenticationMethod(AuthenticationMethod.TOKEN);
            var token = new ClusterModel.TokenAuthentication();
            token.setToken(charArrayOrNull(textFieldToken.getText()));
            schemaRegistry.setTokenAuthentication(token);
            schemaRegistry.setBasicAuthentication(null);
            schemaRegistry.setSslAuthentication(null);
        }
        if (radioButtonSslAuthentication.isSelected()) {
            schemaRegistry.setAuthenticationMethod(AuthenticationMethod.SSL);
            schemaRegistry.setSslAuthentication(getSsl());
            schemaRegistry.setBasicAuthentication(null);
            schemaRegistry.setTokenAuthentication(null);
        }
        schemaRegistry.setAdditionalProperties(ValidateUtils.getAdditionalProperties(textAreaAdditionalProperties));
    }

    boolean isDefined() {
        return StringUtils.isNotBlank(textFieldUrl.getText());
    }

    void validate() {
        try {
            validateTextFieldUrl();
            validateTextAreaAdditionalProperties();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(i18nService.get("common.unknownError"));
        }
    }

    private void validateTextFieldUrl() {
        if (ValidateUtils.isNotUrl(textFieldUrl.getText())) {
            var message = i18nService.get("common.checkParameter").formatted("URL");
            throw new IllegalArgumentException(message);
        }
    }

    private void validateTextAreaAdditionalProperties() {
        try {
            ValidateUtils.validateAdditionalProperties(textAreaAdditionalProperties);
        } catch (IllegalArgumentException e) {
            var message = i18nService.get("common.checkParameter").formatted(i18nService.get("common.additionalProperties"));
            throw new IllegalArgumentException(message);
        }
    }
}
