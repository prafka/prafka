package com.prafka.desktop.controller.model;

import com.prafka.core.model.Config;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import org.apache.commons.lang3.StringUtils;

public class ConfigModelView extends AbstractTableModelView {

    private final Config source;
    private final SimpleObjectProperty<Label> name = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Label> value = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Label> dataType = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Label> sourceType = new SimpleObjectProperty<>();

    public ConfigModelView(Config source) {
        this.source = source;
        var nameLabel = JavaFXUtils.label(source.getName(), "font-code-medium");
        if (StringUtils.isNotBlank(source.getDocumentation())) {
            nameLabel.setTooltip(JavaFXUtils.tooltip(source.getName() + "\n\n" + source.getDocumentation()));
        }
        name.set(nameLabel);
        var prettyValue = prettyValue(source.getName(), source.getValue());
        value.set(JavaFXUtils.labelWithTooltip(prettyValue));
        dataType.set(JavaFXUtils.label(source.getDataType().name().toLowerCase(), "font-code", "font-purple"));
        sourceType.set(JavaFXUtils.label(source.getSourceType().name(), "badge", switch (source.getSourceType()) {
            case DYNAMIC_TOPIC_CONFIG,
                 DYNAMIC_BROKER_LOGGER_CONFIG,
                 DYNAMIC_BROKER_CONFIG,
                 DYNAMIC_DEFAULT_BROKER_CONFIG,
                 DYNAMIC_CLIENT_METRICS_CONFIG,
                 DYNAMIC_GROUP_CONFIG -> "badge-violet";
            case STATIC_BROKER_CONFIG -> "badge-blue";
            case DEFAULT_CONFIG, UNKNOWN -> "badge-gray";
        }));
    }

    public Config getSource() {
        return source;
    }

    public SimpleObjectProperty<Label> nameProperty() {
        return name;
    }

    public SimpleObjectProperty<Label> valueProperty() {
        return value;
    }

    public SimpleObjectProperty<Label> dataTypeProperty() {
        return dataType;
    }

    public SimpleObjectProperty<Label> sourceTypeProperty() {
        return sourceType;
    }

    private static String prettyValue(String name, String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        if (value.equals(String.valueOf(Long.MAX_VALUE))) {
            return "Infinity";
        } else if (name.endsWith(".ms") && !value.startsWith("-")) {
            try {
                return FormatUtils.prettyDurationInMs(value);
            } catch (Exception ignored) {
                return value;
            }
        } else if (name.endsWith(".bytes") && !value.startsWith("-")) {
            try {
                return FormatUtils.prettySizeInBytes(value);
            } catch (Exception ignored) {
                return value;
            }
        } else {
            return value;
        }
    }
}
