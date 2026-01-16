package com.prafka.desktop;

import com.google.inject.Guice;
import com.prafka.core.manager.KafkaManager;
import com.prafka.desktop.concurrent.ServiceAdapter;
import com.prafka.desktop.di.GuiceModule;
import com.prafka.desktop.manager.ViewManager;
import com.prafka.desktop.service.AnalyticsService;
import com.prafka.desktop.service.LogService;
import com.prafka.desktop.service.OnboardingService;
import com.sun.javafx.application.LauncherImpl;
import javafx.stage.Stage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Application extends javafx.application.Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    static final AtomicReference<Optional<Stage>> preloaderStage = new AtomicReference<>(Optional.empty());

    public static void main(String[] args) {
        LauncherImpl.launchApplication(Application.class, ApplicationPreloader.class, args);
    }

    private ViewManager viewManager;
    private KafkaManager kafkaManager;
    private OnboardingService onboardingService;
    private AnalyticsService analyticsService;

    @Override
    public void init() {
        try {
            var injector = Guice.createInjector(new GuiceModule(this));
            viewManager = injector.getInstance(ViewManager.class);
            kafkaManager = injector.getInstance(KafkaManager.class);
            onboardingService = injector.getInstance(OnboardingService.class);
            analyticsService = injector.getInstance(AnalyticsService.class);
            ServiceAdapter.task(() -> injector.getInstance(LogService.class).init()).start();
            viewManager.warmupQuick();
            ServiceAdapter.task(() -> viewManager.warmupLong()).start();
        } catch (Exception e) {
            ExceptionUtils.getRootCause(e).printStackTrace();
            throw e;
        }
    }

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception", throwable);
            analyticsService.uncaughtException(throwable);
        });
        if (onboardingService.onboardingCompleted()) {
            viewManager.showEnterMasterPasswordView(stage);
        } else {
            viewManager.showOnboardingSettingsView(stage);
        }
        preloaderStage.get().ifPresent(Stage::close);
        log.info("Prafka start");
    }

    @Override
    public void stop() throws Exception {
        log.info("Prafka stop");
        kafkaManager.close();
        viewManager.clear();
        com.prafka.core.service.ExecutorHolder.close();
        com.prafka.desktop.service.ExecutorHolder.close();
    }
}