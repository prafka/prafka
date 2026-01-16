package com.prafka.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class CollectionUtils {

    public static Map<Object, Object> mapOf(Object... input) {
        var map = new LinkedHashMap<>();
        for (int i = 0; i < input.length; i += 2) {
            map.put(input[i], input[i + 1]);
        }
        return map;
    }
}
