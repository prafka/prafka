package com.prafka.desktop.controller.connect;

import com.prafka.core.model.Connector;
import com.prafka.core.service.ConnectService;
import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;

import static com.prafka.core.util.StreamUtils.tryIgnore;
import static com.prafka.desktop.util.JavaFXUtils.labelWithTooltip;
import static com.prafka.desktop.util.JavaFXUtils.setNumberLabelLoader;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for displaying the list of Kafka Connect connectors in a searchable table.
 *
 * <p>Shows connector name, connect cluster, plugin, type (sink/source), state, topics,
 * and task counts. Supports filtering by connect cluster and connector operations
 * (pause, resume, restart, delete). Uses rate limiting for fetching connector details.
 */
public class ConnectorListController extends AbstractTableController<List<Connector.Name>, Connector.Name, ConnectorListController.ConnectorModelView> {

    public Button buttonCreateConnector;
    public Pane paneSummaryBlock;
    public ComboBox<ClusterModel.ConnectModel> comboBoxConnects;

    private final ConnectSummaryLoader connectSummaryLoader;
    private final ConnectService connectService;
    private static final RateLimiter rateLimiter = RateLimiterRegistry.of(RateLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(60))
                    .limitRefreshPeriod(Duration.ofMillis(100))
                    .limitForPeriod(1)
                    .build())
            .rateLimiter("kafka-connect");

    @Inject
    public ConnectorListController(ConnectSummaryLoader connectSummaryLoader, ConnectService connectService) {
        this.connectSummaryLoader = connectSummaryLoader;
        this.connectService = connectService;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        buttonCreateConnector.setOnAction(actionEvent ->
                viewManager.showCreateConnectorView(JavaFXUtils.getStage(actionEvent), () -> {
                    sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorCreated"));
                    loadData();
                })
        );

        comboBoxConnects.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClusterModel.ConnectModel connect) {
                return connect == null ? null : connect.getName();
            }

            @Override
            public ClusterModel.ConnectModel fromString(String string) {
                return null;
            }
        });
        comboBoxConnects.setOnAction(it -> loadTableData());
    }

    @Override
    public void initUi() {
        super.initUi();
        comboBoxConnects.getItems().clear();
        var allConnects = new ClusterModel.ConnectModel();
        allConnects.setName(i18nService.get("common.all"));
        allConnects.setId("all");
        comboBoxConnects.getItems().add(allConnects);
        comboBoxConnects.getItems().addAll(sessionService.getCluster().getConnects().stream().sorted(Comparator.comparing(ClusterModel.ConnectModel::getCreatedAt).reversed()).toList());
        comboBoxConnects.getSelectionModel().select(0);
    }

    @Override
    protected void initTable() {
        var columnName = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("common.name"));
        columnName.setCellValueFactory(it -> it.getValue().nameProperty());
        columnName.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnName.setMinWidth(150);

        var columnConnect = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("connect.connect"));
        columnConnect.setCellValueFactory(it -> it.getValue().connectProperty());
        columnConnect.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnConnect.setMinWidth(150);

        var columnPlugin = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("common.plugin"));
        columnPlugin.setCellValueFactory(it -> it.getValue().pluginProperty());
        columnPlugin.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnPlugin.setMinWidth(150);

        var columnType = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("common.type"));
        columnType.setCellValueFactory(it -> it.getValue().typeProperty());
        columnType.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnType.setPrefWidth(100);

        var columnState = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("common.state"));
        columnState.setCellValueFactory(it -> it.getValue().stateProperty());
        columnState.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnState.setPrefWidth(150);

        var columnTopics = JavaFXUtils.<ConnectorModelView, NumberLabel>tableColumn(i18nService.get("common.topics"));
        columnTopics.setCellValueFactory(it -> it.getValue().topicsProperty());
        columnTopics.setComparator(NumberLabel.COMPARATOR);
        columnTopics.setPrefWidth(120);

        var columnTasks = JavaFXUtils.<ConnectorModelView, Label>tableColumn(i18nService.get("common.tasks"));
        columnTasks.setCellValueFactory(it -> it.getValue().tasksProperty());
        columnTasks.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnTasks.setPrefWidth(120);

        var columnActions = JavaFXUtils.<ConnectorModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnType, columnState, columnTopics, columnTasks, columnActions).multiply(0.9);
        columnName.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnPlugin.prefWidthProperty().bind(remainTableWidth.multiply(0.3));
        columnConnect.prefWidthProperty().bind(remainTableWidth.multiply(0.3));

        //noinspection unchecked
        tableView.getColumns().addAll(columnName, columnConnect, columnPlugin, columnType, columnState, columnTopics, columnTasks, columnActions);

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                eventService.fire(new EventService.DashboardContentEvent(viewManager.loadConnectorView(newValue.getCn())));
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(connectSummaryLoader.load(() -> JavaFXUtils.getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<List<Connector.Name>> getLoadTableDataFuture() {
        var connect = Strings.CS.equals(comboBoxConnects.getValue().getId(), "all") ? Optional.<ClusterModel.ConnectModel>empty() : Optional.of(comboBoxConnects.getValue());
        return connect.map(it -> connectService.getAllNames(clusterId(), it.getId())).orElseGet(() -> connectService.getAllNames(clusterId()));
    }

    @Override
    protected List<Map.Entry<Connector.Name, ConnectorModelView>> mapLoadTableDataSource(List<Connector.Name> nameList) {
        return nameList.stream()
                .sorted(Comparator.comparing(Connector.Name::name))
                .map(it -> Map.entry(it, new ConnectorModelView(it)))
                .toList();
    }

    @Override
    protected void loadTableFullData() {
        modelMap.keySet().forEach(cn -> {
            var task = FutureServiceAdapter.futureTask(() -> RateLimiter.decorateFuture(rateLimiter, () -> connectService.get(clusterId(), cn)).get())
                    .onSuccess(connector -> {
                        var model = modelMap.get(cn);
                        if (model != null) {
                            model.setSource(connector);
                            model.setType(connector);
                            model.setPlugin(connector);
                            model.setState(connector);
                            model.setTopics(connector);
                            model.setTasks(connector);
                        }
                    })
                    .onError(it -> {
                        var model = modelMap.get(cn);
                        if (model != null) {
                            JavaFXUtils.setLabelNA(model.typeProperty(), model.pluginProperty(), model.stateProperty(), model.tasksProperty());
                            JavaFXUtils.setNumberLabelNA(model.topicsProperty());
                        }
                        loadDataError(it);
                    })
                    .startNow();
            futureTasks.add(task);
        });
    }

    @Override
    protected boolean getFilterTableDataPredicate(ConnectorModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        if (Strings.CI.contains(model.getName(), search)) {
            return true;
        }
        return false;
    }

    public class ConnectorModelView extends AbstractTableModelView {

        private final Connector.Name cn;
        private Connector source;
        private final SimpleObjectProperty<Label> name = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> type = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> plugin = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> state = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> topics = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> tasks = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> connect = new SimpleObjectProperty<>();

        public ConnectorModelView(Connector.Name cn) {
            this.cn = cn;
            this.name.set(JavaFXUtils.labelWithTooltip(cn.name(), "font-medium"));
            sessionService.getCluster().getConnects().stream()
                    .filter(it -> Strings.CS.equals(it.getId(), cn.connectId()))
                    .findFirst().ifPresent(connect -> this.connect.set(labelWithTooltip(connect.getName(), JavaFXUtils.tooltip(connect.getUrl()))));
            JavaFXUtils.setLabelLoader(themeService.getIconLoader16(), type, plugin, state, tasks);
            setNumberLabelLoader(themeService.getIconLoader16(), topics);
            setActions();
        }

        @Override
        protected void setActions() {
            Runnable update = () ->
                    ServiceAdapter.task(() -> tryIgnore(() -> Thread.sleep(500))) // todo make better
                            .onSuccess(it -> {
                                FutureServiceAdapter.futureTask(() -> connectService.get(clusterId(), cn))
                                        .onSuccess(connector -> {
                                            setSource(connector);
                                            setState(connector);
                                            setTasks(connector);
                                        })
                                        .start();
                                connectSummaryLoader.load(nodes -> paneSummaryBlock.getChildren().setAll(nodes));
                            })
                            .start();

            var menuItemPause = new MenuItem(i18nService.get("connect.pauseConnector"));
            menuItemPause.setOnAction(actionEvent -> {
                FutureServiceAdapter.futureTask(() -> connectService.pause(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorPaused"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start();
            });

            var menuItemResume = new MenuItem(i18nService.get("connect.resumeConnector"));
            menuItemResume.setOnAction(actionEvent -> {
                FutureServiceAdapter.futureTask(() -> connectService.resume(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorResumed"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start();
            });

            var menuItemRestart = new MenuItem(i18nService.get("connect.restartConnector"));
            menuItemRestart.setOnAction(actionEvent -> {
                FutureServiceAdapter.futureTask(() -> connectService.restart(clusterId(), cn))
                        .onSuccess(it -> {
                            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorRestarted"));
                            update.run();
                        })
                        .onError(it -> {
                            update.run();
                            // todo handle error
                        })
                        .start();
            });

            var menuItemDelete = new MenuItem(i18nService.get("connect.deleteConnector"));
            menuItemDelete.setOnAction(actionEvent ->
                    viewManager.showDeleteConnectorConfirmView(JavaFXUtils.getStage(actionEvent), confirmCallback ->
                            FutureServiceAdapter.futureTask(() -> connectService.delete(clusterId(), cn))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_RIGHT, i18nService.get("connect.connectorDeleted"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );

            if (source == null) {
                actions.set(sceneService.createCellActionsMenuButton(menuItemDelete));
            } else {
                var items = new ArrayList<MenuItem>();
                if (source.getState() == Connector.State.RUNNING) items.add(menuItemPause);
                if (source.getState() == Connector.State.PAUSED) items.add(menuItemResume);
                items.add(menuItemRestart);
                items.add(menuItemDelete);
                actions.set(sceneService.createCellActionsMenuButton(items));
            }
        }

        public Connector.Name getCn() {
            return cn;
        }

        public String getName() {
            return cn.name();
        }

        public void setSource(Connector source) {
            this.source = source;
            setActions();
        }

        public SimpleObjectProperty<Label> nameProperty() {
            return name;
        }

        public SimpleObjectProperty<Label> typeProperty() {
            return type;
        }

        public void setType(Connector connector) {
            type.set(JavaFXUtils.label(connector.getType().name(), "badge", switch (connector.getType()) {
                case SINK -> "badge-blue";
                case SOURCE -> "badge-violet";
            }));
        }

        public SimpleObjectProperty<Label> pluginProperty() {
            return plugin;
        }

        public void setPlugin(Connector connector) {
            plugin.set(JavaFXUtils.labelWithTooltip(connector.getPlugin().getClassShort(), JavaFXUtils.tooltip(connector.getPlugin().getClassFull()), "font-code"));
        }

        public SimpleObjectProperty<Label> stateProperty() {
            return state;
        }

        public void setState(Connector connector) {
            state.set(JavaFXUtils.label(connector.getState().name(), "badge", switch (connector.getState()) {
                case RUNNING -> "badge-green";
                case FAILED, TASK_FAILED -> "badge-red";
                case PAUSED -> "badge-yellow";
                case UNASSIGNED -> "badge-gray";
            }));
        }

        public SimpleObjectProperty<NumberLabel> topicsProperty() {
            return topics;
        }

        public void setTopics(Connector connector) {
            var label = JavaFXUtils.numberLabel(connector.getTopics().size(), "font-code");
            if (CollectionUtils.isNotEmpty(connector.getTopics())) {
                var items = String.join("\n", connector.getTopics());
                label.setTooltip(JavaFXUtils.tooltip(items));
                label.setContentDisplay(ContentDisplay.RIGHT);
                themeService.setIcon(label, themeService.getIcon16("information_circle.png"));
            }
            topics.set(label);
        }

        public SimpleObjectProperty<Label> tasksProperty() {
            return tasks;
        }

        public void setTasks(Connector connector) {
            var runCount = connector.getTasks().stream().filter(it -> it.getState() == Connector.Task.State.RUNNING).count();
            var label = JavaFXUtils.label(runCount + " / " + connector.getTasks().size(), "font-code");
            label.setTooltip(JavaFXUtils.tooltip(i18nService.get("connect.runAll")));
            label.setContentDisplay(ContentDisplay.RIGHT);
            themeService.setIcon(label, themeService.getIcon16("information_circle.png"));
            tasks.set(label);
        }

        public SimpleObjectProperty<Label> connectProperty() {
            return connect;
        }
    }
}
