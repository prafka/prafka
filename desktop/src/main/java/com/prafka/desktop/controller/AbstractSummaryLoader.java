package com.prafka.desktop.controller;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import static com.prafka.desktop.util.JavaFXUtils.setPaneLoader;

public abstract class AbstractSummaryLoader extends AbstractController {

    protected Node card(String title, Pane contentPane) {
        return card(title, contentPane, true);
    }

    protected Node card(String title, Pane contentPane, boolean contentPaneLoader) {
        var card = new HBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("summary-card");
        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("title");
        contentPane.getStyleClass().add("content");
        if (contentPaneLoader) setPaneLoader(themeService.getIconLoader16(), contentPane);
        card.getChildren().addAll(titleLabel, contentPane);
        return card;
    }
}
