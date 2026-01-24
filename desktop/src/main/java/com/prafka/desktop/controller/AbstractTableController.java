package com.prafka.desktop.controller;

import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.BooleanUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

/**
 * Base class for controllers displaying data in TableView components.
 *
 * <p>Provides common functionality for table data loading, filtering, sorting,
 * checkbox selection management, and async data operations.
 */
public abstract class AbstractTableController<Source, Key, Model extends AbstractTableModelView> extends AbstractController {

    @FXML
    public Pane paneFilterBlock;
    @FXML
    public TextField textFieldSearch;
    @FXML
    public TableView<Model> tableView;
    @FXML
    public Label labelEmptyTableView;
    @FXML
    public ProgressIndicator progressIndicator;

    protected final Map<Key, Model> modelMap = new LinkedHashMap<>();
    protected final ObservableList<Model> modelObservableList = FXCollections.observableArrayList();
    protected final FilteredList<Model> modelFilteredList = new FilteredList<>(modelObservableList, it -> true);

    protected final CheckBox checkBoxHeader = JavaFXUtils.cellCheckBock();
    protected final SimpleBooleanProperty anyCheckBoxSelected = new SimpleBooleanProperty(false);

    @Override
    public void initFxml() {
        textFieldSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTableData());
        initTable();
        postInitTable();
    }

    @Override
    public void initUi() {
        checkBoxHeader.setSelected(false);
        anyCheckBoxSelected.setValue(false);
        textFieldSearch.setText(null);
        modelMap.clear();
        modelObservableList.clear();
        labelEmptyTableView.setText("");
    }

    @Override
    public void initData() {
        loadData();
    }

    protected abstract void initTable();

    protected void postInitTable() {
        JavaFXUtils.disableTableViewFocus(tableView);

        var modelSortedList = new SortedList<>(modelFilteredList);
        modelSortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(modelSortedList);

        var tableListProperty = new SimpleListProperty<Model>();
        tableListProperty.bind(tableView.itemsProperty());
        tableListProperty.emptyProperty().addListener((observable, oldValue, newValue) ->
                labelEmptyTableView.setText(BooleanUtils.isTrue(newValue) ? i18nService.get("common.noData") : "")
        );

        checkBoxHeader.setOnAction(actionEvent -> {
            anyCheckBoxSelected.setValue(checkBoxHeader.isSelected());
            tableView.getItems().forEach(it -> it.getCheckBox().setSelected(checkBoxHeader.isSelected()));
        });
    }

    protected void loadData() {
        JavaFXUtils.clearTasks(futureTasks);
        loadTableData();
    }

    protected abstract CompletionStage<Source> getLoadTableDataFuture();

    protected abstract List<Map.Entry<Key, Model>> mapLoadTableDataSource(Source source);

    protected void loadTableData() {
        if (disableLoadData) return;
        checkBoxHeader.setSelected(false);
        anyCheckBoxSelected.setValue(false);
        modelMap.clear();
        modelObservableList.clear();
        labelEmptyTableView.setText("");
        progressIndicator.setVisible(true);
        var task = futureTask(this::getLoadTableDataFuture)
                .onSuccess(source -> {
                    mapLoadTableDataSource(source).forEach(entry -> {
                        setModelCheckBoxListener(entry.getValue());
                        modelMap.put(entry.getKey(), entry.getValue());
                    });
                    modelObservableList.setAll(modelMap.values());
                    filterTableData();
                    progressIndicator.setVisible(false);
                    if (modelObservableList.isEmpty()) {
                        labelEmptyTableView.setText(i18nService.get("common.noData"));
                    } else {
                        loadTableFullData();
                    }
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    labelEmptyTableView.setText(i18nService.get("common.noData"));
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task);
    }

    protected void silentLoadTableData() {
        if (disableLoadData) return;
        futureTask(this::getLoadTableDataFuture)
                .onSuccess(source -> {
                    var mappedData = mapLoadTableDataSource(source);
                    if (mappedData.isEmpty()) return;
                    checkBoxHeader.setSelected(false);
                    anyCheckBoxSelected.setValue(false);
                    modelMap.clear();
                    mappedData.forEach(entry -> {
                        setModelCheckBoxListener(entry.getValue());
                        modelMap.put(entry.getKey(), entry.getValue());
                    });
                    modelObservableList.setAll(modelMap.values());
                    loadTableFullData();
                })
                .start();
    }

    protected void loadTableFullData() {
    }

    protected abstract boolean getFilterTableDataPredicate(Model model);

    protected void filterTableData() {
        if (disableLoadData) return;
        checkBoxHeader.setSelected(false);
        anyCheckBoxSelected.setValue(false);
        tableView.getItems().forEach(it -> it.getCheckBox().setSelected(false));
        modelFilteredList.setPredicate(this::getFilterTableDataPredicate);
    }

    protected void setModelCheckBoxListener(Model model) {
        model.getCheckBox().setOnAction(actionEvent -> {
            if (model.getCheckBox().isSelected()) {
                anyCheckBoxSelected.setValue(true);
                checkBoxHeader.setSelected(tableView.getItems().stream().allMatch(it -> it.getCheckBox().isSelected()));
            } else {
                anyCheckBoxSelected.setValue(tableView.getItems().stream().anyMatch(it -> it.getCheckBox().isSelected()));
                checkBoxHeader.setSelected(false);
            }
        });
    }
}
