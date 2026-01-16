package com.prafka.desktop.di;

import com.google.inject.AbstractModule;
import com.prafka.core.manager.KafkaManager;
import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.manager.ApplicationKafkaManager;
import com.prafka.desktop.service.OpenLinkService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;

public class GuiceModule extends AbstractModule {

    private final Application application;

    public GuiceModule(Application application) {
        this.application = application;
    }

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(FXMLLoaderProvider.class);
        bind(KafkaManager.class).to(ApplicationKafkaManager.class);
        bind(ApplicationProperties.class).toInstance(new ApplicationProperties(application));
        bind(OpenLinkService.class).toInstance(new OpenLinkService(application));
    }
}
