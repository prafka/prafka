package com.prafka.desktop.di;

import com.google.inject.Injector;
import com.google.inject.Provider;
import jakarta.inject.Inject;
import javafx.fxml.FXMLLoader;

public class FXMLLoaderProvider implements Provider<FXMLLoader> {

    private final Injector injector;

    @Inject
    public FXMLLoaderProvider(Injector injector) {
        this.injector = injector;
    }

    @Override
    public FXMLLoader get() {
        var loader = new FXMLLoader();
        loader.setControllerFactory(injector::getInstance);
        return loader;
    }
}
