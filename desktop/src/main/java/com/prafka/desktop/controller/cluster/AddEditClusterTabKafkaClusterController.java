package com.prafka.desktop.controller.cluster;

import com.prafka.core.connection.AuthenticationMethod;
import com.prafka.core.connection.SaslMechanism;
import com.prafka.core.connection.SaslSecurityProtocol;
import com.prafka.core.manager.KafkaManager;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ConnectionPropertiesService;
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

import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.label;
import static com.prafka.desktop.util.JavaFXUtils.requestFocus;
import static com.prafka.desktop.util.ValidateUtils.getAdditionalProperties;
import static com.prafka.desktop.util.ValidateUtils.validateAdditionalProperties;

public class AddEditClusterTabKafkaClusterController extends AbstractAddEditClusterTabController {

    public TextField textFieldName;
    public TextField textFieldBootstrapServers;
    public RadioButton radioButtonNoneAuthentication;
    public RadioButton radioButtonSaslAuthentication;
    public RadioButton radioButtonSslAuthentication;
    public Pane paneAuthentication;
    public ComboBox<SaslSecurityProtocol> comboBoxSaslProtocol;
    public ComboBox<SaslMechanism> comboBoxSaslMechanism;
    public TextField textFieldSaslUsername;
    public PasswordField passwordFieldSaslPassword;
    public TextArea textAreaAdditionalProperties;
    public Button buttonTestConnection;

    private final KafkaManager kafkaManager;
    private final ConnectionPropertiesService connectionPropertiesService;
    private AddEditClusterController parentController;
    private Optional<ClusterModel> cluster;

    @Inject
    public AddEditClusterTabKafkaClusterController(KafkaManager kafkaManager, ConnectionPropertiesService connectionPropertiesService) {
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

        comboBoxSaslProtocol = new ComboBox<>();
        comboBoxSaslProtocol.getItems().setAll(SaslSecurityProtocol.values());
        comboBoxSaslProtocol.getSelectionModel().select(0);

        comboBoxSaslMechanism = new ComboBox<>();
        comboBoxSaslMechanism.getItems().setAll(SaslMechanism.PLAIN, SaslMechanism.SCRAM_SHA_256, SaslMechanism.SCRAM_SHA_512);
        comboBoxSaslMechanism.getSelectionModel().select(0);

        textFieldSaslUsername = new TextField();
        passwordFieldSaslPassword = new PasswordField();

        radioButtonNoneAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneAuthentication.getChildren().clear();
        });
        radioButtonSaslAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneSaslAuthentication();
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
            task(getTaskForTestConnection())
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
        if (cluster.isEmpty()) {
            textFieldName.setText("New cluster");
            requestFocus(textFieldBootstrapServers);
        }

        cluster.ifPresent(cluster -> {
            textFieldName.setText(cluster.getName());
            textFieldBootstrapServers.setText(cluster.getBootstrapServers());
            switch (cluster.getAuthenticationMethod()) {
                case NONE -> radioButtonNoneAuthentication.setSelected(true);
                case SASL -> {
                    radioButtonSaslAuthentication.setSelected(true);
                    comboBoxSaslProtocol.getSelectionModel().select(cluster.getSaslAuthentication().getSecurityProtocol());
                    comboBoxSaslMechanism.getSelectionModel().select(cluster.getSaslAuthentication().getSaslMechanism());
                    textFieldSaslUsername.setText(cluster.getSaslAuthentication().getUsername());
                    passwordFieldSaslPassword.setText(stringOrNull(cluster.getSaslAuthentication().getPassword()));
                }
                case SSL -> {
                    radioButtonSslAuthentication.setSelected(true);
                    fillSslFields(cluster.getSslAuthentication());
                }
            }
            textAreaAdditionalProperties.setText(getAdditionalProperties(cluster.getAdditionalProperties()));
        });
    }

    private void fillPaneSaslAuthentication() {
        var pane = new GridPane();
        pane.getColumnConstraints().addAll(new ColumnConstraints() {{
            setPercentWidth(50);
        }}, new ColumnConstraints() {{
            setPercentWidth(50);
        }});

        var securityProtocol = new VBox(5, label(i18nService.get("createClusterView.securityProtocol"), "font-medium"), comboBoxSaslProtocol);
        comboBoxSaslProtocol.prefWidthProperty().bind(securityProtocol.widthProperty());
        GridPane.setMargin(securityProtocol, new Insets(0, 10, 10, 0));

        var mechanism = new VBox(5, label(i18nService.get("createClusterView.saslMechanism"), "font-medium"), comboBoxSaslMechanism);
        comboBoxSaslMechanism.prefWidthProperty().bind(mechanism.widthProperty());
        GridPane.setMargin(mechanism, new Insets(0, 0, 10, 10));

        var username = new VBox(5, label(i18nService.get("common.username"), "font-medium"), textFieldSaslUsername);
        GridPane.setMargin(username, new Insets(0, 10, 0, 0));

        var password = new VBox(5, label(i18nService.get("common.password"), "font-medium"), passwordFieldSaslPassword);
        GridPane.setMargin(password, new Insets(0, 0, 0, 10));

        pane.add(securityProtocol, 0, 0);
        pane.add(mechanism, 1, 0);
        pane.add(username, 0, 1);
        pane.add(password, 1, 1);

        VBox.setMargin(pane, new Insets(20, 0, 0, 0));

        paneAuthentication.getChildren().setAll(pane);
    }

    FailableSupplier<String, Exception> getTaskForTestConnection() {
        return () -> {
            var cluster = new ClusterModel();
            fillClusterModel(cluster);
            try (var adminClient = kafkaManager.createAdminClient(connectionPropertiesService.getKafkaProperties(cluster))) {
                return adminClient.describeCluster().clusterId().get();
            }
        };
    }

    void fillClusterModel(ClusterModel cluster) {
        cluster.setName(textFieldName.getText());
        cluster.setBootstrapServers(textFieldBootstrapServers.getText());
        if (radioButtonNoneAuthentication.isSelected()) {
            cluster.setAuthenticationMethod(AuthenticationMethod.NONE);
            cluster.setSaslAuthentication(null);
            cluster.setSslAuthentication(null);
        }
        if (radioButtonSaslAuthentication.isSelected()) {
            cluster.setAuthenticationMethod(AuthenticationMethod.SASL);
            var sasl = new ClusterModel.SaslAuthentication();
            sasl.setSecurityProtocol(comboBoxSaslProtocol.getValue());
            sasl.setSaslMechanism(comboBoxSaslMechanism.getValue());
            sasl.setUsername(textFieldSaslUsername.getText());
            sasl.setPassword(charArrayOrNull(passwordFieldSaslPassword.getText()));
            cluster.setSaslAuthentication(sasl);
            cluster.setSslAuthentication(null);
        }
        if (radioButtonSslAuthentication.isSelected()) {
            cluster.setAuthenticationMethod(AuthenticationMethod.SSL);
            cluster.setSslAuthentication(getSsl());
            cluster.setSaslAuthentication(null);
        }
        cluster.setAdditionalProperties(getAdditionalProperties(textAreaAdditionalProperties));
    }

    void validate() {
        try {
            validateTextFieldName();
            validateTextFieldBootstrapServers();
            validateTextAreaAdditionalProperties();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(i18nService.get("common.unknownError"));
        }
    }

    private void validateTextFieldName() {
        if (StringUtils.isBlank(textFieldName.getText())) {
            var message = i18nService.get("common.checkParameter").formatted(i18nService.get("common.name"));
            throw new IllegalArgumentException(message);
        }
    }

    private void validateTextFieldBootstrapServers() {
        if (StringUtils.isBlank(textFieldBootstrapServers.getText())) {
            var message = i18nService.get("common.checkParameter").formatted(i18nService.get("createClusterView.labelBootstrapServers"));
            throw new IllegalArgumentException(message);
        }
    }

    private void validateTextAreaAdditionalProperties() {
        try {
            validateAdditionalProperties(textAreaAdditionalProperties);
        } catch (IllegalArgumentException e) {
            var message = i18nService.get("common.checkParameter").formatted(i18nService.get("common.additionalProperties"));
            throw new IllegalArgumentException(message);
        }
    }
}
