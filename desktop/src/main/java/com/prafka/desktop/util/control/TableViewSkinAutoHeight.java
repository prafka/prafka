package com.prafka.desktop.util.control;

import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableViewSkin;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * Custom TableView skin that automatically sizes based on visible row count.
 *
 * <p>Calculates preferred height to display up to the specified maximum
 * number of rows without scrolling.
 */
public class TableViewSkinAutoHeight<T> extends TableViewSkin<T> {

    private final int maxVisibleRows;

    public TableViewSkinAutoHeight(TableView<T> control, int maxVisibleRows) {
        super(control);
        this.maxVisibleRows = maxVisibleRows;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getFlowPrefHeight(maxVisibleRows) + getTableHeaderRow().prefHeight(width);
    }

    protected double getFlowPrefHeight(int rows) {
        double height = 0;
        for (int i = 0; i < rows && i < getItemCount(); i++) {
            height += invokeFlowCellLength(i);
        }
        return height + snappedTopInset() + snappedBottomInset();
    }

    protected double invokeFlowCellLength(int index) {
        try {
            return (double) MethodUtils.invokeMethod(getVirtualFlow(), true, "getCellLength", new Object[]{index}, new Class[]{Integer.TYPE});
        } catch (Exception e) {
            return 0;
        }
    }
}
