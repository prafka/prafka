package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.model.Schema;
import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.apache.commons.lang3.StringUtils;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.*;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * Controller for the schema structure tab showing parsed fields.
 *
 * <p>Displays schema fields in a hierarchical tree table view with field names
 * and types. Supports version selection to view historical schema structures.
 */
public class SchemaTabStructureController extends AbstractSchemaTabController {

    public TreeTableView<StructureModelView> treeTableView;
    public Label labelEmptyTableView;
    public ProgressIndicator progressIndicator;

    private final SchemaRegistryService schemaRegistryService;

    @Inject
    public SchemaTabStructureController(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService = schemaRegistryService;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        comboBoxVersion.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null && !disableChangeVersion) {
                treeTableView.setRoot(null);
                labelEmptyTableView.setText("");
                progressIndicator.setVisible(true);
                futureTask(() -> schemaRegistryService.get(clusterId(), subject, Integer.parseInt(newValue)))
                        .onSuccess(schema -> {
                            progressIndicator.setVisible(false);
                            if (schema.getFields().isEmpty()) {
                                labelEmptyTableView.setText(i18nService.get("common.noData"));
                            } else {
                                treeTableView.setRoot(toRoot(schema));
                            }
                        })
                        .onError(it -> {
                            progressIndicator.setVisible(false);
                            labelEmptyTableView.setText(i18nService.get("common.noData"));
                            loadDataError(it);
                        })
                        .start();
            }
        }));

        initializeTableView();
    }

    @Override
    public void initData() {
        loadData();
    }

    private void initializeTableView() {
        var columnField = JavaFXUtils.<StructureModelView, String>treeTableColumn(i18nService.get("common.field"));
        columnField.setCellValueFactory(it -> it.getValue().getValue().fieldProperty());
        columnField.setSortable(false);
        columnField.setMinWidth(300);

        var columnType = JavaFXUtils.<StructureModelView, Label>treeTableColumn(i18nService.get("common.type"));
        columnType.setCellValueFactory(it -> it.getValue().getValue().typeProperty());
        columnType.setSortable(false);
        columnType.setMinWidth(300);

        var remainTableWidth = getRemainTreeTableWidth(treeTableView).multiply(0.9);
        columnField.prefWidthProperty().bind(remainTableWidth.divide(2));
        columnType.prefWidthProperty().bind(remainTableWidth.divide(2));

        //noinspection unchecked
        treeTableView.getColumns().addAll(columnField, columnType);
        disableTreeTableViewFocus(treeTableView);
    }

    @Override
    protected void loadData() {
        progressIndicator.setVisible(true);
        futureTask(() -> schemaRegistryService.get(clusterId(), subject))
                .onSuccess(schema -> {
                    progressIndicator.setVisible(false);
                    if (schema.getFields().isEmpty()) {
                        labelEmptyTableView.setText(i18nService.get("common.noData"));
                    } else {
                        treeTableView.setRoot(toRoot(schema));
                    }
                    disableChangeVersion = true;
                    comboBoxVersion.getItems().setAll(schema.getVersions().stream().map(String::valueOf).toList());
                    comboBoxVersion.getSelectionModel().select(schema.getVersions().size() - 1);
                    disableChangeVersion = false;
                })
                .onError(it -> {
                    progressIndicator.setVisible(false);
                    labelEmptyTableView.setText(i18nService.get("common.noData"));
                    loadDataError(it);
                })
                .start();
    }

    private TreeItem<StructureModelView> toRoot(Schema schema) {
        var root = new TreeItem<StructureModelView>();
        root.setExpanded(true);
        root.getChildren().addAll(schema.getFields().stream().map(this::toItem).toList());
        return root;
    }

    private TreeItem<StructureModelView> toItem(Schema.Field field) {
        var item = new TreeItem<>(new StructureModelView(field));
        item.setExpanded(true);
        if (isNotEmpty(field.fields())) {
            item.getChildren().addAll(field.fields().stream().map(this::toItem).toList());
        }
        return item;
    }

    public static class StructureModelView {

        private final SimpleStringProperty field = new SimpleStringProperty();
        private final SimpleObjectProperty<Label> type = new SimpleObjectProperty<>();

        public StructureModelView(Schema.Field source) {
            field.set(source.name());
            type.set(label(StringUtils.join(source.types(), ", "), "font-code", "font-purple"));
        }

        public SimpleStringProperty fieldProperty() {
            return field;
        }

        public SimpleObjectProperty<Label> typeProperty() {
            return type;
        }
    }
}
