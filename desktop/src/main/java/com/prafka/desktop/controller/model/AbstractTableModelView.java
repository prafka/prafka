package com.prafka.desktop.controller.model;

import com.prafka.desktop.util.JavaFXUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

public abstract class AbstractTableModelView {

    private final CheckBox checkBoxInternal = JavaFXUtils.cellCheckBock();
    protected final SimpleObjectProperty<Node> checkBox = new SimpleObjectProperty<>(checkBoxInternal);
    protected final SimpleObjectProperty<Node> actions = new SimpleObjectProperty<>();

    protected void setActions() {
    }

    public CheckBox getCheckBox() {
        return checkBoxInternal;
    }

    public SimpleObjectProperty<Node> checkBoxProperty() {
        return checkBox;
    }

    public SimpleObjectProperty<Node> actionsProperty() {
        return actions;
    }
}
