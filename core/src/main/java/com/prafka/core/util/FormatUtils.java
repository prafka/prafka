package com.prafka.core.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import static org.apache.commons.lang3.StringUtils.normalizeSpace;

public class FormatUtils {

    public static String prettyConfigValue(String name, String value) {
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
}
