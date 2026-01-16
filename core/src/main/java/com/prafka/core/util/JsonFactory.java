package com.prafka.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class JsonFactory {

    public static final TypeToken<Map<String, String>> MAP_STING_STRING_TYPE = new TypeToken<>() {
    };
    public static final TypeToken<Map<String, Object>> MAP_STING_OBJECT_TYPE = new TypeToken<>() {
    };

    public static final Gson gsonDefault = new GsonBuilder()
            .registerTypeAdapterFactory(GsonOptionalAdapter.FACTORY)
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    public static final Gson gsonPretty = new GsonBuilder()
            .registerTypeAdapterFactory(GsonOptionalAdapter.FACTORY)
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static final ObjectMapper objectMapperDefault = new ObjectMapper();
}
