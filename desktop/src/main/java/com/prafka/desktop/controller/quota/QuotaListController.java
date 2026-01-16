package com.prafka.desktop.controller.quota;

import com.prafka.core.model.Quota;
import com.prafka.core.service.QuotaService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getRemainTableWidth;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class QuotaListController extends AbstractTableController<List<Quota>, String, QuotaListController.QuotaModelView> {

    public Button buttonCreateQuota;
    public Pane paneSummaryBlock;

    private final QuotaService quotaService;
    private final QuotaSummaryLoader quotaSummaryLoader;

    @Inject
    public QuotaListController(QuotaService quotaService, QuotaSummaryLoader quotaSummaryLoader) {
        this.quotaService = quotaService;
        this.quotaSummaryLoader = quotaSummaryLoader;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        buttonCreateQuota.setOnAction(it -> {
            var stage = JavaFXUtils.getStage(it);
            viewManager.showCreateQuotaView(stage, () -> {
                sceneService.showSnackbarSuccess(stage, Pos.BOTTOM_RIGHT, i18nService.get("quota.quotaCreated"));
                loadData();
            });
        });
    }

    @Override
    protected void initTable() {
        var columnEntityType = JavaFXUtils.<QuotaModelView, Label>tableColumn(i18nService.get("quota.entityType"));
        columnEntityType.setCellValueFactory(it -> it.getValue().entityTypeProperty());
        columnEntityType.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnEntityType.setMinWidth(150);

        var columnEntityName = JavaFXUtils.<QuotaModelView, Label>tableColumn(i18nService.get("quota.entityName"));
        columnEntityName.setCellValueFactory(it -> it.getValue().entityNameProperty());
        columnEntityName.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnEntityName.setMinWidth(200);

        var columnConfigName = JavaFXUtils.<QuotaModelView, Label>tableColumn(i18nService.get("common.property"));
        columnConfigName.setCellValueFactory(it -> it.getValue().configNameProperty());
        columnConfigName.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnConfigName.setMinWidth(200);

        var columnConfigValue = JavaFXUtils.<QuotaModelView, NumberLabel>tableColumn(i18nService.get("common.value"));
        columnConfigValue.setCellValueFactory(it -> it.getValue().configValueProperty());
        columnConfigValue.setComparator(NumberLabel.COMPARATOR);
        columnConfigValue.setMinWidth(150);

        var columnActions = JavaFXUtils.<QuotaModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = getRemainTableWidth(tableView, columnActions).multiply(0.9);
        columnEntityType.prefWidthProperty().bind(remainTableWidth.multiply(0.2));
        columnEntityName.prefWidthProperty().bind(remainTableWidth.multiply(0.3));
        columnConfigName.prefWidthProperty().bind(remainTableWidth.multiply(0.3));
        columnConfigValue.prefWidthProperty().bind(remainTableWidth.multiply(0.2));

        //noinspection unchecked
        tableView.getColumns().addAll(columnEntityType, columnEntityName, columnConfigName, columnConfigValue, columnActions);

        tableView.setRowFactory(JavaFXUtils.clickRowFactory(item ->
                viewManager.showEditQuotaView(getStage(tableView), item.getSource(), () -> {
                    sceneService.showSnackbarSuccess(getStage(tableView), Pos.BOTTOM_RIGHT, i18nService.get("quota.quotaUpdated"));
                    loadData();
                })
        ));
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(quotaSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<List<Quota>> getLoadTableDataFuture() {
        return quotaService.getAll(clusterId());
    }

    @Override
    protected List<Map.Entry<String, QuotaModelView>> mapLoadTableDataSource(List<Quota> quotaList) {
        return quotaList.stream()
                .sorted(Comparator.<Quota, String>comparing(it -> it.getEntity().getType()).thenComparing(it -> it.getEntity().getNameFormatted()))
                .map(it -> Map.entry(UUID.randomUUID().toString(), new QuotaModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(QuotaModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var quota = model.getSource();
        if (Strings.CI.contains(quota.getEntity().getType(), search)
                || Strings.CI.contains(quota.getEntity().getName(), search)
                || Strings.CI.contains(quota.getConfig().getName(), search)) {
            return true;
        }
        return false;
    }

    public class QuotaModelView extends AbstractTableModelView {

        private final Quota source;
        private final SimpleObjectProperty<Label> entityType = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> entityName = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> configName = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> configValue = new SimpleObjectProperty<>();

        public QuotaModelView(Quota source) {
            this.source = source;
            entityType.set(JavaFXUtils.label(source.getEntity().getType(), "font-medium"));
            entityName.set(JavaFXUtils.labelWithTooltip(source.getEntity().getNameFormatted()));
            configName.set(JavaFXUtils.label(StringUtils.upperCase(source.getConfig().getName()), "badge", switch (source.getConfig().getInternalType()) {
                case PRODUCER_RATE -> "badge-violet";
                case CONSUMER_RATE -> "badge-blue";
                case CONNECTION_RATE -> "badge-green";
                default -> "badge-gray";
            }));
            configValue.set(JavaFXUtils.numberLabel(source.getConfig().getValue(), "font-code"));
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemDeleteQuota = new MenuItem(i18nService.get("quota.deleteQuota"));
            menuItemDeleteQuota.setOnAction(sourceActionEvent ->
                    viewManager.showDeleteQuotaConfirmView(JavaFXUtils.getStage(sourceActionEvent), confirmCallback ->
                            futureTask(() -> quotaService.delete(clusterId(), source.getEntity().getInternalType(), source.getEntity().getName(), source.getConfig().getInternalType()))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("quota.quotaDeleted"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );
            actions.set(sceneService.createCellActionsMenuButton(menuItemDeleteQuota));
        }

        public Quota getSource() {
            return source;
        }

        public SimpleObjectProperty<Label> entityTypeProperty() {
            return entityType;
        }

        public SimpleObjectProperty<Label> entityNameProperty() {
            return entityName;
        }

        public SimpleObjectProperty<Label> configNameProperty() {
            return configName;
        }

        public SimpleObjectProperty<NumberLabel> configValueProperty() {
            return configValue;
        }
    }
}
