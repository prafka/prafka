package com.prafka.desktop.service;

import com.prafka.desktop.manager.ViewManager;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Collection;
import java.util.List;

@Singleton
public class SceneService {

    private final ViewManager viewManager;
    private final I18nService i18nService;
    private final ThemeService themeService;

    @Inject
    public SceneService(ViewManager viewManager, I18nService i18nService, ThemeService themeService) {
        this.viewManager = viewManager;
        this.i18nService = i18nService;
        this.themeService = themeService;
    }

    public void addLabelSuccess(Pane pane, String message) {
        var label = new Label(message);
        label.getStyleClass().add("font-green");
        pane.getChildren().add(label);
    }

    public void addLabelError(Pane pane, String message) {
        var label = new Label(message);
        label.getStyleClass().add("font-red");
        pane.getChildren().add(label);
    }

    public void addHyperlinkErrorDetailed(Pane pane, Throwable throwable) {
        if (throwable == null) return;
        var hyperlink = new Hyperlink(i18nService.get("common.detailed"));
        hyperlink.setOnMouseClicked(it -> viewManager.showErrorDetailsView(JavaFXUtils.getStage(it), throwable));
        HBox.setMargin(hyperlink, new Insets(0, 0, 0, 5));
        pane.getChildren().add(hyperlink);
    }

    public void addHyperlinkErrorDetailed(Pane pane, String error) {
        var hyperlink = new Hyperlink(i18nService.get("common.detailed"));
        hyperlink.setOnMouseClicked(it -> viewManager.showErrorDetailsView(JavaFXUtils.getStage(it), error));
        HBox.setMargin(hyperlink, new Insets(0, 0, 0, 5));
        pane.getChildren().add(hyperlink);
    }

    public void addPaneAlertError(Pane pane, Throwable throwable) {
        addLabelError(pane, i18nService.get("common.error"));
        addHyperlinkErrorDetailed(pane, throwable);
    }

    public void showSnackbarSuccess(Stage primaryStage, Pos pos, String message) {
        showSnackbar(primaryStage, pos, JavaFXUtils.label(message, "font-green"), 2000);
    }

    public void showSnackbarError(Stage primaryStage, Pos pos, String message, Throwable throwable) {
        var pane = new HBox();
        addLabelError(pane, message);
        addHyperlinkErrorDetailed(pane, throwable);
        showSnackbar(primaryStage, pos, pane, 5000);
    }

    public void showSnackbar(Stage primaryStage, Pos pos, Node node, long hideDelayMs) {
        var content = new VBox(node);
        content.getStyleClass().add("snackbar");
        var popup = new Popup();
        popup.getContent().add(content);
        popup.setOnShown(it -> {
            switch (pos) {
                case BOTTOM_LEFT -> {
                    popup.setX(primaryStage.getX() + 10);
                    popup.setY(primaryStage.getY() + primaryStage.getHeight() - popup.getHeight() - 10);
                }
                case BOTTOM_RIGHT -> {
                    popup.setX(primaryStage.getX() + primaryStage.getWidth() - popup.getWidth() - 10);
                    popup.setY(primaryStage.getY() + primaryStage.getHeight() - popup.getHeight() - 10);
                }
                default -> throw new IllegalArgumentException();
            }
        });
        popup.show(primaryStage);
        var transition = new PauseTransition(Duration.millis(hideDelayMs));
        transition.setOnFinished(it -> popup.hide());
        transition.play();
        content.hoverProperty().addListener((observable, oldValue, newValue) -> {
            if (BooleanUtils.isTrue(newValue)) {
                transition.pause();
            } else {
                transition.play();
            }
        });
    }

    public MenuButton createCellActionsMenuButton(Collection<MenuItem> items) {
        var menuButton = new MenuButton();
        menuButton.setFocusTraversable(false);
        menuButton.getItems().addAll(items);
        menuButton.getStyleClass().addAll("menu-button-icon-only", "menu-button-without-arrow");
        themeService.setIcon(menuButton, themeService.getIcon16("ellipsis_horizontal.png"));
        return menuButton;
    }

    public MenuButton createCellActionsMenuButton(MenuItem... items) {
        return createCellActionsMenuButton(List.of(items));
    }
}
