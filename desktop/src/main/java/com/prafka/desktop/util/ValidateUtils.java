package com.prafka.desktop.util;

import javafx.scene.control.TextArea;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class ValidateUtils {

    public static void validateAdditionalProperties(TextArea textArea) {
        if (StringUtils.isBlank(textArea.getText())) return;
        textArea.getParagraphs().forEach(it -> {
            if (!it.toString().contains("=")) {
                throw new IllegalArgumentException();
            }
        });
    }

    public static LinkedHashMap<String, String> getAdditionalProperties(TextArea textArea) {
        var result = new LinkedHashMap<String, String>();
        if (StringUtils.isBlank(textArea.getText())) return result;
        textArea.getParagraphs().forEach(it -> {
            var line = it.toString();
            var splitIndex = line.indexOf("=");
            var key = line.substring(0, splitIndex);
            var value = (splitIndex + 1 == line.length()) ? "" : line.substring(splitIndex + 1);
            result.put(key, value);
        });
        return result;
    }

    public static String getAdditionalProperties(LinkedHashMap<String, String> map) {
        if (MapUtils.isEmpty(map)) return null;
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    public static boolean isUrl(String str) {
        return StringUtils.isNotBlank(str) && Strings.CS.startsWithAny(str, "http://", "https://");
    }

    public static boolean isNotUrl(String str) {
        return !isUrl(str);
    }
}
