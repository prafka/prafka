package com.prafka.desktop.controller.connect;

import com.prafka.core.model.Connector;
import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static com.prafka.core.util.StreamUtils.tryIgnore;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for the connector tasks tab showing individual task status.
 *
 * <p>Displays task ID, state, worker assignment, and error trace in a searchable table.
 * Supports individual task restart operations with state refresh.
 */
public class ConnectorTabTasksController extends AbstractTableController<Connector, Integer, ConnectorTabTasksController.TaskModelView> {

    private final ConnectService connectService;
    private Connector.Name cn;
    private AtomicReference<Runnable> silentLoadTasks;

    @Inject
    public ConnectorTabTasksController(ConnectService connectService) {
        this.connectService = connectService;
    }

    public void setData(Connector.Name cn, AtomicReference<Runnable> silentLoadTasks) {
        this.cn = cn;
        this.silentLoadTasks = silentLoadTasks;
    }

    @Override
    public void initUi() {
        super.initUi();
        silentLoadTasks.set(this::silentLoadTableData);
    }

    @Override
    protected void initTable() {
        var columnId = JavaFXUtils.<TaskModelView, NumberLabel>tableColumn(i18nService.get("common.id"));
        columnId.setCellValueFactory(it -> it.getValue().idProperty());
        columnId.setComparator(NumberLabel.COMPARATOR);
        columnId.setPrefWidth(100);

        var columnState = JavaFXUtils.<TaskModelView, Label>tableColumn(i18nService.get("common.state"));
        columnState.setCellValueFactory(it -> it.getValue().stateProperty());
        columnState.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnState.setPrefWidth(150);

        var columnWorker = JavaFXUtils.<TaskModelView, Label>tableColumn(i18nService.get("common.worker"));
        columnWorker.setCellValueFactory(it -> it.getValue().workerProperty());
        columnWorker.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnWorker.setMinWidth(200);

        var columnTrace = JavaFXUtils.<TaskModelView, Pane>tableColumn(i18nService.get("common.trace"));
        columnTrace.setCellValueFactory(it -> it.getValue().traceProperty());
        columnTrace.setSortable(false);
        columnTrace.setMinWidth(200);

        var columnActions = JavaFXUtils.<TaskModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnId, columnState, columnActions).multiply(0.9);
        columnWorker.prefWidthProperty().bind(remainTableWidth.multiply(0.5));
        columnTrace.prefWidthProperty().bind(remainTableWidth.multiply(0.5));

        //noinspection unchecked
        tableView.getColumns().addAll(columnId, columnState, columnWorker, columnTrace, columnActions);
    }

    @Override
    protected CompletionStage<Connector> getLoadTableDataFuture() {
        return connectService.get(clusterId(), cn);
    }

    @Override
    protected List<Map.Entry<Integer, TaskModelView>> mapLoadTableDataSource(Connector connector) {
        return connector.getTasks().stream()
                .sorted(Comparator.comparing(Connector.Task::getId))
                .map(it -> Map.entry(it.getId(), new TaskModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(TaskModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        if (Strings.CI.contains(String.valueOf(model.getId()), search)) {
            return true;
        }
        return false;
    }

    public class TaskModelView extends AbstractTableModelView {

        private Connector.Task source;
        private final SimpleObjectProperty<NumberLabel> id = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> worker = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Pane> trace = new SimpleObjectProperty<>();

        public TaskModelView(Connector.Task source) {
            this.source = source;
            id.set(JavaFXUtils.numberLabel(source.getId(), "font-code-medium"));
            setState(source);
            setWorker(source);
            setTrace(source);
            setActions();
        }

        @Override
        protected void setActions() {
            Runnable update = () ->
                    ServiceAdapter.task(() -> tryIgnore(() -> Thread.sleep(500))) // todo make better
                            .onSuccess(it -> {
                                FutureServiceAdapter.futureTask(() -> connectService.get(clusterId(), cn))
                                        .onSuccess(connector -> {
                                            connector.getTasks().stream()
                                                    .filter(task -> source.getId() == task.getId())
                                                    .findFirst()
                                                    .ifPresent(task -> {
                                                        this.source = task;
                                                        setState(task);
                                                        setWorker(task);
                                                        setTrace(task);
                                                    });
                                        })
                                        .start();
                            })
                            .start();

            var menuItemRestart = new MenuItem(i18nService.get("connect.restartTask"));
            menuItemRestart.setOnAction(actionEvent -> {
                FutureServiceAdapter.futureTask(() -> connectService.restartTask(clusterId(), cn, getId()))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.taskRestarted"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start();
            });

            actions.set(sceneService.createCellActionsMenuButton(menuItemRestart));
        }

        public int getId() {
            return source.getId();
        }

        public SimpleObjectProperty<NumberLabel> idProperty() {
            return id;
        }

        public SimpleObjectProperty<Label> workerProperty() {
            return worker;
        }

        public void setWorker(Connector.Task task) {
            worker.set(JavaFXUtils.labelWithTooltip(task.getWorkerId()));
        }

        public SimpleObjectProperty<Label> stateProperty() {
            return state;
        }

        public void setState(Connector.Task task) {
            state.set(JavaFXUtils.label(task.getState().name(), "badge", switch (source.getState()) {
                case RUNNING -> "badge-green";
                case FAILED -> "badge-red";
                case PAUSED, RESTARTING -> "badge-yellow";
                case UNASSIGNED -> "badge-gray";
            }));
        }

        public SimpleObjectProperty<Pane> traceProperty() {
            return trace;
        }

        public void setTrace(Connector.Task task) {
            if (StringUtils.isNotBlank(task.getTrace())) {
                var pane = new BorderPane();
                var center = new HBox(JavaFXUtils.labelWithTooltip(StringUtils.replaceEach(task.getTrace(), new String[]{"\r", "\n"}, new String[]{"", ""}), JavaFXUtils.tooltip(task.getTrace())));
                center.setAlignment(Pos.CENTER_LEFT);
                pane.setCenter(center);
                var right = new HBox();
                right.setAlignment(Pos.CENTER_LEFT);
                sceneService.addHyperlinkErrorDetailed(right, task.getTrace());
                pane.setRight(right);
                trace.set(pane);
            }
        }
    }
}
