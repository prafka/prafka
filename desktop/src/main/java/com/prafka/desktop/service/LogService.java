package com.prafka.desktop.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.prafka.desktop.ApplicationProperties;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.LoggerFactory;

/**
 * Configures and manages application logging using Logback.
 *
 * <p>Initializes logging configuration, manages log levels,
 * and provides access to the log directory.
 */
@Singleton
public class LogService {

    private final ApplicationProperties applicationProperties;
    private final SettingsService settingsService;

    @Inject
    public LogService(ApplicationProperties applicationProperties, SettingsService settingsService) {
        this.applicationProperties = applicationProperties;
        this.settingsService = settingsService;
    }

    public void init() {
        try {
            var context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            context.putProperty("CONSOLE_ENABLED", applicationProperties.isLogConsoleEnabled());
            context.putProperty("FILE_ENABLED", "true");
            context.putProperty("LOG_DIR", applicationProperties.userLogDir());
            var configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(getClass().getResource("/logback-custom.xml"));
            if (settingsService.getLog().isDebug()) {
                setDebug();
            }
        } catch (JoranException e) {
            e.printStackTrace();
        }
    }

    public String getDir() {
        return applicationProperties.userLogDir();
    }

    public void setDebug() {
        getRootLogger().setLevel(Level.DEBUG);
    }

    public void setInfo() {
        getRootLogger().setLevel(Level.INFO);
    }

    private ch.qos.logback.classic.Logger getRootLogger() {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }
}
