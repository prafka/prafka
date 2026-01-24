package com.prafka.desktop.controller.cluster;

import com.prafka.core.connection.AuthenticationMethod;
import com.prafka.core.manager.KafkaManager;
import com.prafka.desktop.model.AbstractTrackedModel;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ConnectionPropertiesService;
import com.prafka.desktop.util.ValidateUtils;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.prafka.core.util.JsonFactory.gsonDefault;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.label;
import static com.prafka.desktop.util.ValidateUtils.getAdditionalProperties;
import static com.prafka.desktop.util.ValidateUtils.validateAdditionalProperties;

/**
 * Controller for the Kafka Connect configuration tab in the add/edit cluster dialog.
 *
 * <p>Supports configuration of multiple Connect instances with name, URL,
 * authentication settings, and connection testing.
 */
public class AddEditClusterTabKafkaConnectController extends AbstractAddEditClusterTabController {

    public Pane paneConnectList;
    public Pane paneButtonAddConnect;
    public TextField textFieldName;
    public TextField textFieldUrl;
    public RadioButton radioButtonNoneAuthentication;
    public RadioButton radioButtonBasicAuthentication;
    public RadioButton radioButtonSslAuthentication;
    public Pane paneAuthentication;
    public TextField textFieldBasicUsername;
    public PasswordField passwordFieldBasicPassword;
    public TextArea textAreaAdditionalProperties;
    public Button buttonTestConnection;

    private final KafkaManager kafkaManager;
    private final ConnectionPropertiesService connectionPropertiesService;
    private final List<ClusterModel.ConnectModel> connectList = new LinkedList<>();
    private final SimpleObjectProperty<Pair<ClusterModel.ConnectModel, Pane>> currentConnect = new SimpleObjectProperty<>();
    private AddEditClusterController parentController;
    private Optional<ClusterModel> cluster;

    @Inject
    public AddEditClusterTabKafkaConnectController(KafkaManager kafkaManager, ConnectionPropertiesService connectionPropertiesService) {
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

        radioButtonNoneAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            paneAuthentication.getChildren().clear();
        });
        radioButtonBasicAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneBasicAuthentication();
        });
        radioButtonSslAuthentication.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isNotTrue(newValue)) return;
            fillPaneSslAuthentication(paneAuthentication);
        });

        var buttonAddConnect = new Button();
        buttonAddConnect.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(buttonAddConnect, Priority.ALWAYS);
        buttonAddConnect.getStyleClass().addAll("secondary", "button-icon-only");
        themeService.setIcon16(buttonAddConnect, "add.png");
        paneButtonAddConnect = new VBox(buttonAddConnect);
        buttonAddConnect.setOnAction(actionEvent -> {
            parentController.paneAlert.getChildren().clear();
            try {
                validate();
            } catch (IllegalArgumentException e) {
                sceneService.addLabelError(parentController.paneAlert, e.getMessage());
                return;
            }
            addNewConnect();
            fillPaneConnectList();
        });

        currentConnect.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                fillConnectModel(oldValue.getKey());
                ((Button) oldValue.getValue().getChildren().get(0)).textProperty().unbind();
                ((Button) oldValue.getValue().getChildren().get(0)).setDefaultButton(false);
            }
            if (newValue != null) {
                fillFields(newValue.getKey());
                ((Button) newValue.getValue().getChildren().get(0)).textProperty().bind(textFieldName.textProperty());
                ((Button) newValue.getValue().getChildren().get(0)).setDefaultButton(true);
            }
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
        if (cluster.isPresent() && cluster.get().isConnectsDefined()) {
            cluster.get().getConnects().stream().sorted(Comparator.comparing(AbstractTrackedModel::getCreatedAt)).forEach(this::addExistingConnect);
        } else {
            addNewConnect();
        }
        fillPaneConnectList();
    }

    private void addNewConnect() {
        var connect = new ClusterModel.ConnectModel();
        connect.setName("New connect");
        connect.setAuthenticationMethod(AuthenticationMethod.NONE);
        connectList.add(connect);
    }

    private void addExistingConnect(ClusterModel.ConnectModel connect) {
        connectList.add(copy(connect));
    }

    private void fillPaneConnectList() {
        paneConnectList.getChildren().setAll(
                connectList.stream()
                        .map(connect -> {
                            var name = new Button();
                            name.getStyleClass().addAll("secondary", "button-line-left");
                            var delete = new Button();
                            delete.setMaxHeight(Double.MAX_VALUE);
                            VBox.setVgrow(delete, Priority.ALWAYS);
                            delete.getStyleClass().addAll("secondary", "button-line-right", "button-icon-only");
                            themeService.setIcon16(delete, "trash.png");
                            var pane = new HBox(name, new VBox(delete));
                            pane.getStyleClass().add("button-line");

                            currentConnect.set(Pair.of(connect, pane));

                            name.setOnAction(actionEvent -> {
                                parentController.paneAlert.getChildren().clear();
                                try {
                                    validate();
                                } catch (IllegalArgumentException e) {
                                    sceneService.addLabelError(parentController.paneAlert, e.getMessage());
                                    return;
                                }
                                currentConnect.set(Pair.of(connect, pane));
                            });

                            delete.setOnAction(actionEvent -> {
                                connectList.removeIf(it -> Strings.CS.equals(it.getId(), connect.getId()));
                                if (connectList.isEmpty()) {
                                    addNewConnect();
                                }
                                fillPaneConnectList();
                            });

                            return pane;
                        })
                        .toList()
        );
        paneConnectList.getChildren().add(paneButtonAddConnect);
    }

    private void fillFields(ClusterModel.ConnectModel connect) {
        textFieldName.setText(connect.getName());
        textFieldUrl.setText(connect.getUrl());
        switch (connect.getAuthenticationMethod()) {
            case NONE -> radioButtonNoneAuthentication.setSelected(true);
            case BASIC -> {
                radioButtonBasicAuthentication.setSelected(true);
                textFieldBasicUsername.setText(connect.getBasicAuthentication().getUsername());
                passwordFieldBasicPassword.setText(stringOrNull(connect.getBasicAuthentication().getPassword()));
            }
            case SSL -> {
                radioButtonSslAuthentication.setSelected(true);
                fillSslFields(connect.getSslAuthentication());
            }
        }
        textAreaAdditionalProperties.setText(getAdditionalProperties(connect.getAdditionalProperties()));
    }

    private void fillPaneBasicAuthentication() {
        var pane = new GridPane();
        pane.getColumnConstraints().addAll(new ColumnConstraints() {{
            setPercentWidth(50);
        }}, new ColumnConstraints() {{
            setPercentWidth(50);
        }});

        var username = new VBox(5, label(i18nService.get("common.username"), "font-medium"), textFieldBasicUsername);
        GridPane.setMargin(username, new Insets(0, 10, 0, 0));

        var password = new VBox(5, label(i18nService.get("common.password"), "font-medium"), passwordFieldBasicPassword);
        GridPane.setMargin(password, new Insets(0, 0, 0, 10));

        pane.add(username, 0, 0);
        pane.add(password, 1, 0);

        VBox.setMargin(pane, new Insets(20, 0, 0, 0));

        paneAuthentication.getChildren().setAll(pane);
    }

    private FailableSupplier<String, Exception> getTaskForTestConnection() {
        return () -> {
            var connect = new ClusterModel.ConnectModel();
            fillConnectModel(connect);
            return kafkaManager.createConnectClient(connectionPropertiesService.getConnectProperties(connect)).getConnectServerVersion().getVersion();
        };
    }

    void fillClusterModel(ClusterModel cluster) {
        if (currentConnect.getValue() != null) fillConnectModel(currentConnect.getValue().getKey());
        var connects = connectList.stream().filter(it -> StringUtils.isNotBlank(it.getName()) && ValidateUtils.isUrl(it.getUrl())).toList();
        if (connects.isEmpty()) {
            cluster.setConnects(null);
            return;
        }
        cluster.setConnects(connects.stream().map(this::copy).toList());
    }

    private void fillConnectModel(ClusterModel.ConnectModel connect) {
        connect.setName(textFieldName.getText());
        connect.setUrl(textFieldUrl.getText());
        if (radioButtonNoneAuthentication.isSelected()) {
            connect.setAuthenticationMethod(AuthenticationMethod.NONE);
            connect.setBasicAuthentication(null);
            connect.setSslAuthentication(null);
        }
        if (radioButtonBasicAuthentication.isSelected()) {
            connect.setAuthenticationMethod(AuthenticationMethod.BASIC);
            var basic = new ClusterModel.BasicAuthentication();
            basic.setUsername(textFieldBasicUsername.getText());
            basic.setPassword(charArrayOrNull(passwordFieldBasicPassword.getText()));
            connect.setBasicAuthentication(basic);
            connect.setSslAuthentication(null);
        }
        if (radioButtonSslAuthentication.isSelected()) {
            connect.setAuthenticationMethod(AuthenticationMethod.SSL);
            connect.setSslAuthentication(getSsl());
            connect.setBasicAuthentication(null);
        }
        connect.setAdditionalProperties(getAdditionalProperties(textAreaAdditionalProperties));
    }

    boolean isDefined() {
        return StringUtils.isNotBlank(textFieldName.getText()) && StringUtils.isNotBlank(textFieldUrl.getText());
    }

    void validate() {
        try {
            validateTextFieldName();
            validateTextFieldUrl();
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

    private void validateTextFieldUrl() {
        if (ValidateUtils.isNotUrl(textFieldUrl.getText())) {
            var message = i18nService.get("common.checkParameter").formatted("URL");
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

    private ClusterModel.ConnectModel copy(ClusterModel.ConnectModel from) {
        return gsonDefault.fromJson(gsonDefault.toJson(from), ClusterModel.ConnectModel.class);
    }
}
