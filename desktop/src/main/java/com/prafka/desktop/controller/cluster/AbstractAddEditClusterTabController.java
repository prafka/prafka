package com.prafka.desktop.controller.cluster;

import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.util.control.RetentionFileChooser;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.apache.kafka.common.config.SslConfigs;

import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static com.prafka.desktop.util.JavaFXUtils.label;

abstract class AbstractAddEditClusterTabController extends AbstractController {

    public TextField textFieldKeystoreLocation;
    public PasswordField passwordFieldKeystorePassword;
    public PasswordField passwordFieldKeyPassword;
    public TextField textFieldTruststoreLocation;
    public PasswordField passwordFieldTruststorePassword;

    @Override
    public void initFxml() {
        textFieldKeystoreLocation = new TextField();
        textFieldKeystoreLocation.setPromptText(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);
        passwordFieldKeystorePassword = new PasswordField();
        passwordFieldKeystorePassword.setPromptText(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG);
        passwordFieldKeyPassword = new PasswordField();
        passwordFieldKeyPassword.setPromptText(SslConfigs.SSL_KEY_PASSWORD_CONFIG);
        textFieldTruststoreLocation = new TextField();
        textFieldTruststoreLocation.setPromptText(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
        passwordFieldTruststorePassword = new PasswordField();
        passwordFieldTruststorePassword.setPromptText(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);
    }

    protected void fillSslFields(ClusterModel.SslAuthentication ssl) {
        textFieldKeystoreLocation.setText(ssl.getKeystoreLocation());
        passwordFieldKeystorePassword.setText(stringOrNull(ssl.getKeystorePassword()));
        passwordFieldKeyPassword.setText(stringOrNull(ssl.getKeyPassword()));
        textFieldTruststoreLocation.setText(ssl.getTruststoreLocation());
        passwordFieldTruststorePassword.setText(stringOrNull(ssl.getTruststorePassword()));
    }

    protected ClusterModel.SslAuthentication getSsl() {
        var ssl = new ClusterModel.SslAuthentication();
        ssl.setKeystoreLocation(textFieldKeystoreLocation.getText());
        ssl.setKeystorePassword(charArrayOrNull(passwordFieldKeystorePassword.getText()));
        ssl.setKeyPassword(passwordFieldKeyPassword.getText() == null ? null : passwordFieldKeyPassword.getText().toCharArray());
        ssl.setTruststoreLocation(textFieldTruststoreLocation.getText());
        ssl.setTruststorePassword(charArrayOrNull(passwordFieldTruststorePassword.getText()));
        return ssl;
    }

    protected void fillPaneSslAuthentication(Pane paneAuthentication) {
        var pane = new VBox(10);

        var buttonOpenKeystore = new Button(i18nService.get("common.open"));
        buttonOpenKeystore.getStyleClass().add("secondary");
        buttonOpenKeystore.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter(i18nService.get("common.allFiles"), "*.*"));
            var file = fileChooser.showOpenDialog(getStage(actionEvent));
            if (file != null) textFieldKeystoreLocation.setText(file.getAbsolutePath());
        });

        HBox.setHgrow(textFieldKeystoreLocation, Priority.ALWAYS);

        var keystorePane = new GridPane();
        keystorePane.getColumnConstraints().addAll(new ColumnConstraints() {{
            setPercentWidth(50);
        }}, new ColumnConstraints() {{
            setPercentWidth(50);
        }});
        keystorePane.add(passwordFieldKeystorePassword, 0, 0);
        GridPane.setMargin(passwordFieldKeystorePassword, new Insets(0, 5, 0, 0));
        keystorePane.add(passwordFieldKeyPassword, 1, 0);
        GridPane.setMargin(passwordFieldKeyPassword, new Insets(0, 0, 0, 5));

        var keystore = new VBox(
                5,
                label("Keystore", "font-medium"),
                new VBox(10, new HBox(10, buttonOpenKeystore, textFieldKeystoreLocation), keystorePane)
        );

        var buttonOpenTruststore = new Button(i18nService.get("common.open"));
        buttonOpenTruststore.getStyleClass().add("secondary");
        buttonOpenTruststore.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter(i18nService.get("common.allFiles"), "*.*"));
            var file = fileChooser.showOpenDialog(getStage(actionEvent));
            if (file != null) textFieldTruststoreLocation.setText(file.getAbsolutePath());
        });

        HBox.setHgrow(textFieldTruststoreLocation, Priority.ALWAYS);

        var truststore = new VBox(
                5,
                label("Truststore", "font-medium"),
                new VBox(10, new HBox(10, buttonOpenTruststore, textFieldTruststoreLocation), passwordFieldTruststorePassword)
        );

        pane.getChildren().addAll(keystore, truststore);

        VBox.setMargin(pane, new Insets(20, 0, 0, 0));

        paneAuthentication.getChildren().setAll(pane);
    }

    protected String stringOrNull(char[] arr) {
        return arr == null ? null : new String(arr);
    }

    protected char[] charArrayOrNull(String str) {
        return str == null ? null : str.toCharArray();
    }
}
