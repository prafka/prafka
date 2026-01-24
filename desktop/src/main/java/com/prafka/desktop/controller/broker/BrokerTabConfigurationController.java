package com.prafka.desktop.controller.broker;

import com.prafka.core.model.Broker;
import com.prafka.core.model.Config;
import com.prafka.core.service.ConfigService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.ConfigModelView;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for the broker configuration tab showing all config properties.
 *
 * <p>Displays broker configuration properties in a searchable table with
 * property name, value, data type, and source information. Supports raw view export.
 */
public class BrokerTabConfigurationController extends AbstractTableController<List<Config>, String, ConfigModelView> {

    public Pane paneFilterBlock;
    public Button buttonRawView;

    private final ConfigService configService;
    private Broker broker;

    @Inject
    public BrokerTabConfigurationController(ConfigService configService) {
        this.configService = configService;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        buttonRawView.setOnAction(it -> viewManager.showRawConfigView(JavaFXUtils.getStage(it), modelObservableList.stream().map(ConfigModelView::getSource).toList()));
    }

    @Override
    protected void initTable() {
        var columnProperty = JavaFXUtils.<ConfigModelView, Label>tableColumn(i18nService.get("common.property"));
        columnProperty.setCellValueFactory(it -> it.getValue().nameProperty());
        columnProperty.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnProperty.setMinWidth(200);

        var columnValue = JavaFXUtils.<ConfigModelView, Label>tableColumn(i18nService.get("common.value"));
        columnValue.setCellValueFactory(it -> it.getValue().valueProperty());
        columnValue.setSortable(false);
        columnValue.setMinWidth(200);

        var columnDataType = JavaFXUtils.<ConfigModelView, Label>tableColumn(i18nService.get("common.type"));
        columnDataType.setCellValueFactory(it -> it.getValue().dataTypeProperty());
        columnDataType.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnDataType.setPrefWidth(150);

        var columnSourceType = JavaFXUtils.<ConfigModelView, Label>tableColumn(i18nService.get("common.source"));
        columnSourceType.setCellValueFactory(it -> it.getValue().sourceTypeProperty());
        columnSourceType.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnSourceType.setPrefWidth(210);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnDataType, columnSourceType).multiply(0.9);
        columnProperty.prefWidthProperty().bind(remainTableWidth.multiply(0.6));
        columnValue.prefWidthProperty().bind(remainTableWidth.multiply(0.4));

        //noinspection unchecked
        tableView.getColumns().addAll(columnProperty, columnValue, columnDataType, columnSourceType);

        tableView.setRowFactory(JavaFXUtils.clickRowFactory(item ->
                viewManager.showEditBrokerConfigView(getStage(tableView), broker, item.getSource())
        ));
    }

    @Override
    protected CompletionStage<List<Config>> getLoadTableDataFuture() {
        return configService.getAllByBroker(clusterId(), broker.getId());
    }

    @Override
    protected List<Map.Entry<String, ConfigModelView>> mapLoadTableDataSource(List<Config> configList) {
        return configList.stream()
                .sorted(Comparator.comparing(Config::getName))
                .map(it -> Map.entry(it.getName(), new ConfigModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(ConfigModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var config = model.getSource();
        if (Strings.CI.contains(config.getName(), search) || Strings.CI.contains(config.getValue(), search)) {
            return true;
        }
        return false;
    }
}
