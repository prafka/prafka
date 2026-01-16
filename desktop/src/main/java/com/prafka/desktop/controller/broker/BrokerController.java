package com.prafka.desktop.controller.broker;

import com.prafka.core.model.Broker;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.service.EventService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class BrokerController extends AbstractController {

    public Label labelH1;
    public Label labelH1Comeback;
    public TabPane tabPane;
    public Tab tabConfiguration;
    public Tab tabLogDirs;

    private Broker broker;

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void initFxml() {
        labelH1Comeback.setOnMouseClicked(it -> eventService.fire(EventService.DashboardEvent.LOAD_BROKERS));

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (disableLoadData) return;
            if (tabConfiguration.getId().equals(newValue.getId())) {
                tabConfiguration.setContent(viewManager.loadBrokerTabConfigurationView(broker));
                return;
            }
            if (tabLogDirs.getId().equals(newValue.getId())) {
                tabLogDirs.setContent(viewManager.loadBrokerTabLogDirsView(broker));
                return;
            }
        });
    }

    @Override
    public void initUi() {
        labelH1.setText(broker.getAddress());
        tabPane.getSelectionModel().select(0);
    }

    @Override
    public void initData() {
        Platform.runLater(() -> tabConfiguration.setContent(viewManager.loadBrokerTabConfigurationView(broker)));
    }
}
