package com.prafka.desktop.di;

import com.google.inject.Injector;
import com.google.inject.Provider;
import jakarta.inject.Inject;
import javafx.fxml.FXMLLoader;

/**
 * Guice provider that creates {@link FXMLLoader} instances with Guice-based controller factory.
 *
 * <p>Integrates JavaFX FXML loading with Guice dependency injection by configuring
 * each loader to use Guice for controller instantiation.
 */
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
