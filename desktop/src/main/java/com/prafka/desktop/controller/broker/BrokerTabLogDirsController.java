package com.prafka.desktop.controller.broker;

import com.prafka.core.model.Broker;
import com.prafka.core.model.LogDir;
import com.prafka.core.service.LogDirService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.NumberLabel;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.TopicPartition;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class BrokerTabLogDirsController extends AbstractTableController<Map<TopicPartition, List<LogDir>>, String, BrokerTabLogDirsController.LogDirModelView> {

    public Pane paneFilterBlock;

    private final LogDirService logDirService;
    private Broker broker;

    @Inject
    public BrokerTabLogDirsController(LogDirService logDirService) {
        this.logDirService = logDirService;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    @Override
    protected void initTable() {
        var columnDirectory = JavaFXUtils.<LogDirModelView, Label>tableColumn(i18nService.get("common.directory"));
        columnDirectory.setCellValueFactory(it -> it.getValue().directoryProperty());
        columnDirectory.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnDirectory.setMinWidth(200);

        var columnTopic = JavaFXUtils.<LogDirModelView, Label>tableColumn(i18nService.get("common.topic"));
        columnTopic.setCellValueFactory(it -> it.getValue().topicProperty());
        columnDirectory.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnTopic.setMinWidth(200);

        var columnPartition = JavaFXUtils.<LogDirModelView, NumberLabel>tableColumn(i18nService.get("common.partition"));
        columnPartition.setCellValueFactory(it -> it.getValue().partitionProperty());
        columnPartition.setComparator(NumberLabel.COMPARATOR);
        columnPartition.setPrefWidth(120);

        var columnSize = JavaFXUtils.<LogDirModelView, NumberLabel>tableColumn(i18nService.get("common.size"));
        columnSize.setCellValueFactory(it -> it.getValue().sizeProperty());
        columnSize.setComparator(NumberLabel.COMPARATOR);
        columnSize.setPrefWidth(120);

        var columnOffsetLag = JavaFXUtils.<LogDirModelView, NumberLabel>tableColumn(i18nService.get("common.offsetLag"));
        columnOffsetLag.setCellValueFactory(it -> it.getValue().offsetLagProperty());
        columnOffsetLag.setComparator(NumberLabel.COMPARATOR);
        columnOffsetLag.setPrefWidth(120);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPartition, columnSize, columnOffsetLag).multiply(0.9);
        columnDirectory.prefWidthProperty().bind(remainTableWidth.multiply(0.5));
        columnTopic.prefWidthProperty().bind(remainTableWidth.multiply(0.5));

        //noinspection unchecked
        tableView.getColumns().addAll(columnDirectory, columnTopic, columnPartition, columnSize, columnOffsetLag);
    }

    @Override
    protected CompletionStage<Map<TopicPartition, List<LogDir>>> getLoadTableDataFuture() {
        return logDirService.getAllByBroker(clusterId(), broker.getId());
    }

    @Override
    protected List<Map.Entry<String, LogDirModelView>> mapLoadTableDataSource(Map<TopicPartition, List<LogDir>> logDirMap) {
        return logDirMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(it -> new LogDirModelView(entry.getKey(), it)))
                .sorted(Comparator.comparing(it -> it.getSource().getPath()))
                .map(it -> Map.entry(UUID.randomUUID().toString(), it))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(LogDirModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var tp = model.getTp();
        var logDir = model.getSource();
        if (Strings.CI.contains(logDir.getPath(), search) || Strings.CI.contains(tp.topic(), search)) {
            return true;
        }
        return false;
    }

    public static class LogDirModelView extends AbstractTableModelView {

        private final TopicPartition tp;
        private final LogDir source;
        private final SimpleObjectProperty<Label> directory = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> topic = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> partition = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> size = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<NumberLabel> offsetLag = new SimpleObjectProperty<>();

        public LogDirModelView(TopicPartition tp, LogDir source) {
            this.tp = tp;
            this.source = source;
            directory.set(JavaFXUtils.labelWithTooltip(source.getPath(), "font-code-medium"));
            topic.set(JavaFXUtils.labelWithTooltip(tp.topic()));
            partition.set(JavaFXUtils.numberLabel(tp.partition(), "font-code"));
            size.set(JavaFXUtils.numberLabelText(source.getSize(), FormatUtils.prettySizeInBytes(source.getSize()), "font-code"));
            offsetLag.set(JavaFXUtils.numberLabel(source.getOffsetLag(), "font-code"));
        }

        public TopicPartition getTp() {
            return tp;
        }

        public LogDir getSource() {
            return source;
        }

        public SimpleObjectProperty<Label> directoryProperty() {
            return directory;
        }

        public SimpleObjectProperty<Label> topicProperty() {
            return topic;
        }

        public SimpleObjectProperty<NumberLabel> partitionProperty() {
            return partition;
        }

        public SimpleObjectProperty<NumberLabel> sizeProperty() {
            return size;
        }

        public SimpleObjectProperty<NumberLabel> offsetLagProperty() {
            return offsetLag;
        }
    }
}
