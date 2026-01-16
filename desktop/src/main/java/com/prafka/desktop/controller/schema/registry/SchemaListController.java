package com.prafka.desktop.controller.schema.registry;

import com.prafka.core.model.Schema;
import com.prafka.core.service.SchemaRegistryService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.Strings;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.FormatUtils.prettyEnum;
import static com.prafka.desktop.util.JavaFXUtils.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SchemaListController extends AbstractTableController<Collection<String>, String, SchemaListController.SchemaModelView> {

    public Button buttonCreateSchema;
    public Pane paneSummaryBlock;

    private final SchemaSummaryLoader schemaSummaryLoader;
    private final SchemaRegistryService schemaRegistryService;
    private static final RateLimiter rateLimiter = RateLimiterRegistry.of(RateLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(60))
                    .limitRefreshPeriod(Duration.ofMillis(100))
                    .limitForPeriod(1)
                    .build())
            .rateLimiter("schema-registry");

    @Inject
    public SchemaListController(SchemaSummaryLoader schemaSummaryLoader, SchemaRegistryService schemaRegistryService) {
        this.schemaSummaryLoader = schemaSummaryLoader;
        this.schemaRegistryService = schemaRegistryService;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        buttonCreateSchema.setOnAction(actionEvent ->
                viewManager.showCreateSchemaView(getStage(actionEvent), () -> {
                    sceneService.showSnackbarSuccess(getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.schemaCreated"));
                    loadData();
                })
        );
    }

    @Override
    protected void initTable() {
        var columnSubject = JavaFXUtils.<SchemaModelView, Label>tableColumn(i18nService.get("common.subjectName"));
        columnSubject.setCellValueFactory(it -> it.getValue().subjectProperty());
        columnSubject.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnSubject.setMinWidth(200);

        var columnRecord = JavaFXUtils.<SchemaModelView, Label>tableColumn(i18nService.get("common.recordName"));
        columnRecord.setCellValueFactory(it -> it.getValue().recordProperty());
        columnRecord.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnRecord.setMinWidth(200);

        var columnType = JavaFXUtils.<SchemaModelView, Label>tableColumn(i18nService.get("common.type"));
        columnType.setCellValueFactory(it -> it.getValue().typeProperty());
        columnType.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnType.setPrefWidth(150);

        var columnCompatibility = JavaFXUtils.<SchemaModelView, Pane>tableColumn(i18nService.get("common.compatibility"));
        columnCompatibility.setCellValueFactory(it -> it.getValue().compatibilityProperty());
        columnCompatibility.setPrefWidth(200);

        var columnVersion = JavaFXUtils.<SchemaModelView, NumberLabel>tableColumn(i18nService.get("common.version"));
        columnVersion.setCellValueFactory(it -> it.getValue().versionProperty());
        columnVersion.setComparator(NumberLabel.COMPARATOR);
        columnVersion.setPrefWidth(110);

        var columnActions = JavaFXUtils.<SchemaModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = getRemainTableWidth(tableView, columnType, columnCompatibility, columnVersion, columnActions).multiply(0.9);
        columnSubject.prefWidthProperty().bind(remainTableWidth.multiply(0.6));
        columnRecord.prefWidthProperty().bind(remainTableWidth.multiply(0.4));

        //noinspection unchecked
        tableView.getColumns().addAll(columnSubject, columnRecord, columnType, columnCompatibility, columnVersion, columnActions);

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                eventService.fire(new EventService.DashboardContentEvent(viewManager.loadSchemaView(newValue.getSubject())));
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(schemaSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<Collection<String>> getLoadTableDataFuture() {
        return schemaRegistryService.getAllSubjects(clusterId());
    }

    @Override
    protected List<Map.Entry<String, SchemaModelView>> mapLoadTableDataSource(Collection<String> subjectList) {
        return subjectList.stream().sorted().map(it -> Map.entry(it, new SchemaModelView(it))).toList();
    }

    @Override
    protected void loadTableFullData() {
        modelMap.keySet().forEach(subject -> {
            var task = futureTask(() -> RateLimiter.decorateFuture(rateLimiter, () -> schemaRegistryService.get(clusterId(), subject)).get())
                    .onSuccess(schema -> {
                        var model = modelMap.get(subject);
                        if (model != null) {
                            model.setSource(schema);
                            model.setType(schema);
                            model.setRecord(schema);
                            model.setCompatibility(schema);
                            model.setVersion(schema);
                        }
                    })
                    .onError(it -> {
                        var model = modelMap.get(subject);
                        if (model != null) {
                            setLabelNA(model.typeProperty(), model.recordProperty());
                            setNumberLabelNA(model.versionProperty());
                            setPaneNA(model.compatibilityProperty().get());
                        }
                        loadDataError(it);
                    })
                    .startNow();
            futureTasks.add(task);
        });
    }

    @Override
    protected boolean getFilterTableDataPredicate(SchemaModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        if (Strings.CI.contains(model.getSubject(), search)) {
            return true;
        }
        return false;
    }

    public class SchemaModelView extends AbstractTableModelView {

        private Schema source;
        private final SimpleObjectProperty<Label> subject = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> type = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> record = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Pane> compatibility = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> version = new SimpleObjectProperty<>();

        public SchemaModelView(String subject) {
            this.subject.set(labelWithTooltip(subject, "font-medium"));
            setLabelLoader(themeService.getIconLoader16(), type, record);
            setNumberLabelLoader(themeService.getIconLoader16(), version);
            compatibility.set(new HBox(labelLoader(themeService.getIconLoader16())));
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemEditCompatibility = new MenuItem(i18nService.get("schema.editCompatibility"));
            menuItemEditCompatibility.setOnAction(sourceActionEvent ->
                    viewManager.showEditSchemaCompatibilityView(getStage(sourceActionEvent), getSubject(), getCompatibility(), () -> {
                        sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.compatibilityEdited"));
                        loadData();
                    })
            );

            var menuItemDeleteSchema = new MenuItem(i18nService.get("schema.deleteSchema"));
            menuItemDeleteSchema.setOnAction(sourceActionEvent ->
                    viewManager.showDeleteSchemaConfirmView(getStage(sourceActionEvent), getSubject(), () -> {
                        sceneService.showSnackbarSuccess(getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("schema.schemaDeleted"));
                        loadData();
                    })
            );

            actions.set(sceneService.createCellActionsMenuButton(menuItemEditCompatibility, menuItemDeleteSchema));
        }

        public void setSource(Schema schema) {
            source = schema;
        }

        public String getSubject() {
            return subject.get().getText();
        }

        public SimpleObjectProperty<Label> subjectProperty() {
            return subject;
        }

        public SimpleObjectProperty<Label> typeProperty() {
            return type;
        }

        public void setType(Schema schema) {
            this.type.set(label(schema.getType().name(), "badge", switch (schema.getType()) {
                case AVRO -> "badge-blue";
                case JSON -> "badge-gray";
                case PROTOBUF -> "badge-green";
            }));
        }

        public SimpleObjectProperty<Label> recordProperty() {
            return record;
        }

        public void setRecord(Schema schema) {
            record.set(labelWithTooltip(schema.getRecord()));
        }

        public CompatibilityLevel getCompatibility() {
            return source != null ? source.getCompatibility().getLevel() : null;
        }

        public SimpleObjectProperty<Pane> compatibilityProperty() {
            return compatibility;
        }

        public void setCompatibility(Schema schema) {
            var pane = new BorderPane();
            pane.setLeft(label(prettyEnum(schema.getCompatibility().getLevel()), "pd-r-0_4"));
            if (schema.getCompatibility().isGlobal()) {
                var box = new HBox(label("(Global)", "label-desc"));
                box.setAlignment(Pos.CENTER_LEFT);
                pane.setCenter(box);
            }
            compatibility.set(pane);
        }

        public SimpleObjectProperty<NumberLabel> versionProperty() {
            return version;
        }

        public void setVersion(Schema schema) {
            version.set(numberLabel(schema.getVersion(), "font-code"));
        }
    }
}
