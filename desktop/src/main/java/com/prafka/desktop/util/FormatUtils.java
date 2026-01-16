package com.prafka.desktop.util;

import com.google.gson.JsonParser;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;

import static com.prafka.core.util.JsonFactory.gsonPretty;
import static com.prafka.desktop.util.JavaFXUtils.label;
import static com.prafka.desktop.util.JavaFXUtils.tooltip;
import static org.apache.commons.lang3.StringUtils.*;

public class FormatUtils {

    public static final String NA = "N/A";

    public static String prettyDurationInMs(long durationInMs) {
        var temp = DurationFormatUtils.formatDuration(durationInMs, "d'd 'H'h 'm'min 's's 'S'ms'")
                .replace("0d", "")
                .replace(" 0h", " ")
                .replace(" 0min", " ")
                .replace(" 0s", " ")
                .replace(" 000ms", "")
                .replace(" 00", " ")
                .replace(" 0", " ");
        return normalizeSpace(temp);
    }

    public static String prettyDurationInMs(String durationInMs) {
        return prettyDurationInMs(Long.parseLong(durationInMs));
    }

    public static String prettySizeInBytes(long sizeInBytes) {
        return FileUtils.byteCountToDisplaySize(sizeInBytes);
    }

    public static String prettySizeInBytes(String sizeInBytes) {
        return prettySizeInBytes(Long.parseLong(sizeInBytes));
    }

    public static String prettyEnum(Enum<?> value) {
        return prettyEnum(value.name());
    }

    public static String prettyEnum(String value) {
        return capitalize(lowerCase(Strings.CS.replace(value, "_", " ")));
    }

    public static String prettyAvro(String avro) {
        return prettyJson(avro);
    }

    public static String prettyJson(String json) {
        try {
            return gsonPretty.toJson(JsonParser.parseString(json));
        } catch (Exception ignored) {
            return json;
        }
    }

    public static Label prettyAclPrincipal(String principal) {
        var splitPrincipal = splitAclPrincipal(principal);
        var group = splitPrincipal.getLeft();
        var item = splitPrincipal.getRight();
        var label = label(item == null ? group : group + ": " + item);
        if (item != null && !"*".equals(item)) label.setTooltip(tooltip(item));
        return label;
    }

    public static Pair<String, String> splitAclPrincipal(String principal) {
        try {
            var separator = principal.indexOf(":");
            var group = principal.substring(0, separator);
            var item = principal.substring(separator + 1);
            return Pair.of(group, item);
        } catch (Exception ignored) {
            return Pair.of(principal, null);
        }
    }
}
