package com.prafka.desktop;

import com.prafka.desktop.service.ThemeService;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

public class ApplicationPreloader extends Preloader {

    @Override
    public void start(Stage stage) throws Exception {
        setApplicationName();
        Application.preloaderStage.set(Optional.of(stage));
        var scene = new Scene(new VBox(new ImageView("/img/preloader.png")));
        stage.setScene(scene);
        stage.setTitle(ApplicationProperties.NAME);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setWidth(600);
        stage.setHeight(400);
        stage.setResizable(false);
        stage.getIcons().setAll(ThemeService.ICONS);
        stage.show();
    }

    private static void setApplicationName() {
        try {
            com.sun.glass.ui.Application.GetApplication().setName(ApplicationProperties.NAME);
        } catch (Exception e) {
            Platform.runLater(() -> com.sun.glass.ui.Application.GetApplication().setName(ApplicationProperties.NAME));
        }
    }
}
