package com.prafka.desktop;

import javafx.application.Application;
import net.harawata.appdirs.AppDirsFactory;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.utils.AppInfoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Manages application configuration properties and user data directories.
 *
 * <p>Loads build-time properties and runtime configuration, including application
 * name, version, URLs, and user-specific paths for data and logs.
 */
public class ApplicationProperties {

    private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    public static final String NAME;
    public static final String VERSION;
    private static final Map<String, String> PROPERTIES = new HashMap<>();

    static {
        loadBuildProperties();
        loadApplicationProperties();
        NAME = PROPERTIES.get("project.build.name");
        VERSION = PROPERTIES.get("project.build.version");
    }

    private static void loadBuildProperties() {
        try (var resourceStream = AppInfoParser.class.getResourceAsStream("/build.properties")) {
            var properties = new Properties();
            properties.load(resourceStream);
            properties.forEach((key, value) -> {
                if (key.toString().startsWith("project.build.")) {
                    PROPERTIES.put(key.toString(), value.toString());
                }
            });
        } catch (Exception e) {
            log.error("Error while loading build.properties", e);
        }
    }

    private static void loadApplicationProperties() {
        try (var resourceStream = AppInfoParser.class.getResourceAsStream("/application.properties")) {
            var properties = new Properties();
            properties.load(resourceStream);
            properties.forEach((key, value) -> {
                var newValue = Strings.CS.replace(value.toString(), "${project.build.domain}", PROPERTIES.get("project.build.domain"));
                PROPERTIES.put(key.toString(), newValue);
            });
        } catch (Exception e) {
            log.error("Error while loading application.properties", e);
        }
    }

    private final String userDataDir;
    private final String userLogDir;

    public ApplicationProperties(Application application) {
        PROPERTIES.putAll(application.getParameters().getNamed());
        userDataDir = PROPERTIES.getOrDefault("userDataDir", AppDirsFactory.getInstance().getUserDataDir("prafka", "v1", "prafkacom"));
        userLogDir = PROPERTIES.getOrDefault("userLogDir", AppDirsFactory.getInstance().getUserDataDir("prafka", "v1", "prafkacom"));
        prepareUserDir(userDataDir);
        prepareUserDir(userLogDir);
    }

    private void prepareUserDir(String userDir) {
        try {
            var dir = Path.of(userDir);
            if (Files.notExists(dir)) Files.createDirectories(dir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String isLogConsoleEnabled() {
        return PROPERTIES.getOrDefault("logConsoleEnabled", "false");
    }

    public boolean isTrackStageSize() {
        return PROPERTIES.getOrDefault("trackStageSize", "true").equals("true");
    }

    public String userDataDir() {
        return userDataDir;
    }

    public String userLogDir() {
        return userLogDir;
    }

    public String domain() {
        return PROPERTIES.get("project.build.domain");
    }

    public String distUrl() {
        return PROPERTIES.get("distUrl");
    }

    public String apiUrl() {
        return PROPERTIES.get("apiUrl");
    }

    public String docsUrl() {
        return PROPERTIES.get("docsUrl");
    }

    public String email() {
        return PROPERTIES.get("email");
    }
}
