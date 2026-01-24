package com.prafka.desktop.controller.model;

import com.prafka.desktop.util.JavaFXUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

/**
 * Base class for table row view models with selection and action support.
 *
 * <p>Provides checkbox for row selection and actions property for row-level
 * context menus in TableView components.
 */
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
