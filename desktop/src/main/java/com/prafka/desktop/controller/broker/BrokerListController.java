package com.prafka.desktop.controller.broker;

import com.prafka.core.model.Broker;
import com.prafka.core.model.LogDir;
import com.prafka.core.service.BrokerService;
import com.prafka.core.service.LogDirService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.service.EventService;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for displaying the list of Kafka brokers in a searchable table.
 *
 * <p>Shows broker ID, rack, address, partition count, and storage size with
 * summary cards for overall cluster broker information.
 */
public class BrokerListController extends AbstractTableController<List<Broker>, Integer, BrokerListController.BrokerModelView> {

    public Pane paneSummaryBlock;

    private final BrokerSummaryLoader brokerSummaryLoader;
    private final BrokerService brokerService;
    private final LogDirService logDirService;

    @Inject
    public BrokerListController(BrokerSummaryLoader brokerSummaryLoader, BrokerService brokerService, LogDirService logDirService) {
        this.brokerSummaryLoader = brokerSummaryLoader;
        this.brokerService = brokerService;
        this.logDirService = logDirService;
    }

    @Override
    protected void initTable() {
        var columnId = JavaFXUtils.<BrokerModelView, NumberLabel>tableColumn(i18nService.get("common.id"));
        columnId.setCellValueFactory(it -> it.getValue().idProperty());
        columnId.setComparator(NumberLabel.COMPARATOR);
        columnId.setPrefWidth(100);

        var columnRack = JavaFXUtils.<BrokerModelView, Label>tableColumn(i18nService.get("common.rack"));
        columnRack.setCellValueFactory(it -> it.getValue().rackProperty());
        columnRack.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnRack.setPrefWidth(100);

        var columnAddress = JavaFXUtils.<BrokerModelView, Node>tableColumn(i18nService.get("common.address"));
        columnAddress.setCellValueFactory(it -> it.getValue().addressProperty());
        columnAddress.setComparator(JavaFXUtils.BORDER_PANE_LEFT_LABEL_COMPARATOR);
        columnAddress.setMinWidth(200);

        var columnPartitions = JavaFXUtils.<BrokerModelView, NumberLabel>tableColumn(i18nService.get("common.partitions"));
        columnPartitions.setCellValueFactory(it -> it.getValue().partitionsProperty());
        columnPartitions.setComparator(NumberLabel.COMPARATOR);
        columnPartitions.setPrefWidth(130);

        var columnSize = JavaFXUtils.<BrokerModelView, NumberLabel>tableColumn(i18nService.get("common.size"));
        columnSize.setCellValueFactory(it -> it.getValue().sizeProperty());
        columnSize.setComparator(NumberLabel.COMPARATOR);
        columnSize.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnId, columnRack, columnPartitions, columnSize).multiply(0.9);
        columnAddress.prefWidthProperty().bind(remainTableWidth);

        //noinspection unchecked
        tableView.getColumns().addAll(columnId, columnRack, columnAddress, columnPartitions, columnSize);

        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                eventService.fire(new EventService.DashboardContentEvent(viewManager.loadBrokerView(newValue.getSource())));
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(brokerSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<List<Broker>> getLoadTableDataFuture() {
        return brokerService.getAll(clusterId());
    }

    @Override
    protected List<Map.Entry<Integer, BrokerModelView>> mapLoadTableDataSource(List<Broker> brokerList) {
        return brokerList.stream()
                .sorted(Comparator.comparing(Broker::getId))
                .map(it -> Map.entry(it.getId(), new BrokerModelView(it)))
                .toList();
    }

    @Override
    protected void loadTableFullData() {
        var task = futureTask(() -> logDirService.getAllByBrokers(clusterId(), modelMap.keySet()))
                .onSuccess(logDirMap -> {
                    modelMap.forEach((id, model) -> {
                        var logDir = logDirMap.get(id);
                        if (logDir != null) {
                            model.setPartitions(logDir);
                            model.setSize(logDir);
                        } else {
                            JavaFXUtils.setNumberLabelNA(model.partitionsProperty(), model.sizeProperty());
                        }
                    });
                })
                .onError(it -> {
                    modelMap.values().forEach(model -> JavaFXUtils.setNumberLabelNA(model.partitionsProperty(), model.sizeProperty()));
                    loadDataError(it);
                })
                .startNow();
        futureTasks.add(task);
    }

    @Override
    protected boolean getFilterTableDataPredicate(BrokerModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var broker = model.getSource();
        if (Strings.CI.contains(String.valueOf(broker.getId()), search)
                || Strings.CI.contains(broker.getHost(), search)
                || Strings.CI.contains(String.valueOf(broker.getPort()), search)) {
            return true;
        }
        return false;
    }

    public class BrokerModelView extends AbstractTableModelView {

        private final Broker source;
        private final SimpleObjectProperty<NumberLabel> id = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> rack = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Node> address = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partitions = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> size = new SimpleObjectProperty<>();

        public BrokerModelView(Broker source) {
            this.source = source;
            id.set(JavaFXUtils.numberLabel(source.getId(), "font-code-medium"));
            if (isBlank(source.getRack())) {
                var label = new Label();
                themeService.setIcon16(label, "ban.png");
                rack.set(label);
            } else {
                rack.set(JavaFXUtils.label(source.getRack()));
            }
            var pane = new BorderPane();
            pane.setLeft(JavaFXUtils.labelWithTooltip(source.getAddress(), "font-medium", "pd-r-0_4"));
            if (source.isController()) {
                var box = new HBox(JavaFXUtils.label(i18nService.get("common.controller").toUpperCase(), "badge", "badge-blue"));
                box.setAlignment(Pos.CENTER_LEFT);
                pane.setCenter(box);
            }
            address.set(pane);
            JavaFXUtils.setNumberLabelLoader(themeService.getIconLoader16(), partitions, size);
        }

        public Broker getSource() {
            return source;
        }

        public int getId() {
            return id.get().getSource().intValue();
        }

        public SimpleObjectProperty<NumberLabel> idProperty() {
            return id;
        }

        public SimpleObjectProperty<Label> rackProperty() {
            return rack;
        }

        public SimpleObjectProperty<Node> addressProperty() {
            return address;
        }

        public SimpleObjectProperty<NumberLabel> partitionsProperty() {
            return partitions;
        }

        public void setPartitions(Map<TopicPartition, List<LogDir>> logDirs) {
            partitions.set(JavaFXUtils.numberLabel(logDirs.size(), "font-code"));
        }

        public SimpleObjectProperty<NumberLabel> sizeProperty() {
            return size;
        }

        public void setSize(Map<TopicPartition, List<LogDir>> logDirs) {
            var sizeInBytes = logDirs.values().stream().flatMap(Collection::stream).map(LogDir::getSize).reduce(0L, Long::sum);
            size.set(JavaFXUtils.numberLabelText(sizeInBytes, FormatUtils.prettySizeInBytes(sizeInBytes), "font-code"));
        }
    }
}
