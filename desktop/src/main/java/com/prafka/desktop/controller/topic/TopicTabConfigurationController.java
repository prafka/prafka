package com.prafka.desktop.controller.topic;

import com.prafka.core.model.Config;
import com.prafka.core.service.ConfigService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.ConfigModelView;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.clients.admin.ConfigEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TopicTabConfigurationController extends AbstractTableController<List<Config>, String, ConfigModelView> {

    public CheckBox checkBoxShowOverridesOnly;
    public Button buttonRawView;

    private final ConfigService configService;
    private String topicName;

    @Inject
    public TopicTabConfigurationController(ConfigService configService) {
        this.configService = configService;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        checkBoxShowOverridesOnly.setOnAction(it -> filterTableData());
        buttonRawView.setOnAction(actionEvent -> viewManager.showRawConfigView(JavaFXUtils.getStage(actionEvent), modelObservableList.stream().map(ConfigModelView::getSource).toList()));
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
                viewManager.showEditTopicConfigView(getStage(tableView), topicName, item.getSource(), () -> {
                    sceneService.showSnackbarSuccess(getStage(tableView), Pos.BOTTOM_RIGHT, i18nService.get("topic.configUpdated"));
                    loadData();
                })
        ));
    }

    @Override
    protected CompletionStage<List<Config>> getLoadTableDataFuture() {
        return configService.getAllByTopic(clusterId(), topicName);
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
        var config = model.getSource();
        var search = textFieldSearch.getText();
        if (isBlank(search)) {
            if (checkBoxShowOverridesOnly.isSelected()) {
                return config.getSourceType() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
            } else {
                return true;
            }
        }
        if (Strings.CI.contains(config.getName(), search) || Strings.CI.contains(config.getValue(), search)) {
            if (checkBoxShowOverridesOnly.isSelected()) {
                return config.getSourceType() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
            } else {
                return true;
            }
        }
        return false;
    }
}
