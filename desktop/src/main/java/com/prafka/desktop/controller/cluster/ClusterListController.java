package com.prafka.desktop.controller.cluster;

import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.ClusterService;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.RetentionFileChooser;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for displaying and managing the list of configured Kafka clusters.
 *
 * <p>Provides functionality to view, add, edit, delete, import, and export
 * cluster configurations in a searchable table view.
 */
public class ClusterListController extends AbstractTableController<List<ClusterModel>, String, ClusterListController.ClusterModelView> {

    public Button buttonImport;
    public Button buttonExport;
    public Button buttonAddCluster;

    private final ClusterService clusterService;

    @Inject
    public ClusterListController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        buttonImport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showOpenDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> clusterService.importClusters(Files.readString(Path.of(file.getAbsolutePath()))))
                    .onSuccess(it -> {
                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.imported"));
                        loadData();
                        eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS_SILENT);
                    })
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.error"), it))
                    .start();
        });

        buttonExport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.setInitialFileName("clusters.json");
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showSaveDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> Files.writeString(Path.of(file.getAbsolutePath()), clusterService.exportClusters()))
                    .onSuccess(it -> sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.exported")))
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("common.error"), it))
                    .start();
        });

        buttonAddCluster.setOnAction(actionEvent ->
                viewManager.showAddClusterView(JavaFXUtils.getStage(actionEvent), () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterAdded"));
                    loadData();
                    eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                })
        );
    }

    @Override
    protected void initTable() {
        var columnName = JavaFXUtils.<ClusterModelView, Node>tableColumn(i18nService.get("common.name"));
        columnName.setCellValueFactory(it -> it.getValue().nameProperty());
        columnName.setComparator(JavaFXUtils.BORDER_PANE_LEFT_LABEL_COMPARATOR);
        columnName.setMinWidth(200);

        var columnBootstrapServers = JavaFXUtils.<ClusterModelView, Label>tableColumn(i18nService.get("cluster.bootstrapServers"));
        columnBootstrapServers.setCellValueFactory(it -> it.getValue().bootstrapServersProperty());
        columnBootstrapServers.setSortable(false);
        columnBootstrapServers.setMinWidth(200);

        var columnSchemaRegistry = JavaFXUtils.<ClusterModelView, Label>tableColumn(i18nService.get("common.schemaRegistry"));
        columnSchemaRegistry.setCellValueFactory(it -> it.getValue().schemaRegistryProperty());
        columnSchemaRegistry.setSortable(false);
        columnSchemaRegistry.setMinWidth(200);

        var columnConnects = JavaFXUtils.<ClusterModelView, Label>tableColumn(i18nService.get("common.kafkaConnect"));
        columnConnects.setCellValueFactory(it -> it.getValue().connectsProperty());
        columnConnects.setSortable(false);
        columnConnects.setPrefWidth(150);

        var columnActions = JavaFXUtils.<ClusterModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnConnects, columnActions).multiply(0.9);
        columnName.prefWidthProperty().bind(remainTableWidth.multiply(0.3));
        columnBootstrapServers.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnSchemaRegistry.prefWidthProperty().bind(remainTableWidth.multiply(0.3));

        //noinspection unchecked
        tableView.getColumns().addAll(columnName, columnBootstrapServers, columnSchemaRegistry, columnConnects, columnActions);

        tableView.setRowFactory(JavaFXUtils.clickRowFactory(item ->
                viewManager.showEditClusterView(getStage(tableView), item.getSource(), () -> {
                    sceneService.showSnackbarSuccess(getStage(tableView), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterUpdated"));
                    loadData();
                    eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                })
        ));
    }

    @Override
    protected CompletionStage<List<ClusterModel>> getLoadTableDataFuture() {
        return CompletableFuture.supplyAsync(clusterService::getClusters);
    }

    @Override
    protected List<Map.Entry<String, ClusterModelView>> mapLoadTableDataSource(List<ClusterModel> clusterList) {
        return clusterList.stream()
                .sorted(Comparator.comparing(ClusterModel::getName))
                .map(it -> Map.entry(it.getId(), new ClusterModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(ClusterModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var cluster = model.getSource();
        if (Strings.CI.contains(cluster.getName(), search) || Strings.CI.contains(cluster.getBootstrapServers(), search)) {
            return true;
        }
        return false;
    }

    public class ClusterModelView extends AbstractTableModelView {

        private final ClusterModel source;
        private final SimpleObjectProperty<Node> name = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> bootstrapServers = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> schemaRegistry = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> connects = new SimpleObjectProperty<>();

        public ClusterModelView(ClusterModel source) {
            this.source = source;
            var pane = new BorderPane();
            pane.setLeft(JavaFXUtils.labelWithTooltip(source.getName(), "font-medium", "pd-r-0_4"));
            if (source.isCurrent()) {
                var box = new HBox(JavaFXUtils.label(i18nService.get("common.current").toUpperCase(), "badge", "badge-blue"));
                box.setAlignment(Pos.CENTER_LEFT);
                pane.setCenter(box);
            }
            name.set(pane);
            bootstrapServers.set(JavaFXUtils.labelWithTooltip(source.getBootstrapServers()));
            if (source.isSchemaRegistryDefined()) {
                schemaRegistry.set(JavaFXUtils.labelWithTooltip(source.getSchemaRegistry().getUrl()));
            } else {
                var label = new Label();
                label.setTooltip(JavaFXUtils.tooltip(i18nService.get("common.notDefined")));
                themeService.setIcon16(label, "ban.png");
                schemaRegistry.set(label);
            }
            if (source.isConnectsDefined()) {
                connects.set(JavaFXUtils.labelWithTooltip(
                        source.getConnects().size(),
                        JavaFXUtils.tooltip(String.join("\n", source.getConnects().stream().map(ClusterModel.ConnectModel::getUrl).toList())),
                        "font-code"
                ));
            } else {
                var label = new Label();
                label.setTooltip(JavaFXUtils.tooltip(i18nService.get("common.notDefined")));
                themeService.setIcon16(label, "ban.png");
                connects.set(label);
            }
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemDeleteCluster = new MenuItem(i18nService.get("cluster.deleteCluster"));
            menuItemDeleteCluster.setOnAction(sourceActionEvent ->
                    viewManager.showDeleteClusterConfirmView(JavaFXUtils.getStage(sourceActionEvent), confirmCallback ->
                            task(() -> clusterService.deleteCluster(source))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("cluster.clusterDeleted"));
                                        loadData();
                                        eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );
            menuItemDeleteCluster.setDisable(source.isCurrent());
            actions.set(sceneService.createCellActionsMenuButton(menuItemDeleteCluster));
        }

        public ClusterModel getSource() {
            return source;
        }

        public SimpleObjectProperty<Node> nameProperty() {
            return name;
        }

        public SimpleObjectProperty<Label> bootstrapServersProperty() {
            return bootstrapServers;
        }

        public SimpleObjectProperty<Label> schemaRegistryProperty() {
            return schemaRegistry;
        }

        public SimpleObjectProperty<Label> connectsProperty() {
            return connects;
        }
    }
}
