package com.prafka.core.service;

import com.prafka.core.model.SerdeType;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.datafaker.Faker;
import net.datafaker.providers.base.BaseProviders;
import net.datafaker.providers.entertainment.EntertainmentProviders;
import net.datafaker.providers.food.FoodProviders;
import net.datafaker.providers.videogame.VideoGameProviders;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating random Kafka record data for testing purposes.
 *
 * <p>Uses the Datafaker library to generate realistic random JSON payloads
 * and supports random generation for various serialization types including
 * strings, numeric types, UUIDs, and JSON.
 *
 * @see SerdeType
 */
@Named
@Singleton
public class RecordRandomService {

    private static final Faker FAKER = new Faker();
    private static final List<Method> FAKER_PROVIDERS = new ArrayList<>();

    static {
        var methods = new ArrayList<Method>();
        methods.addAll(Arrays.asList(BaseProviders.class.getDeclaredMethods()));
        methods.addAll(Arrays.asList(FoodProviders.class.getDeclaredMethods()));
        methods.addAll(Arrays.asList(EntertainmentProviders.class.getDeclaredMethods()));
        methods.addAll(Arrays.asList(VideoGameProviders.class.getDeclaredMethods()));
        methods.stream()
                .filter(it -> Modifier.isPublic(it.getModifiers()) && it.getParameterCount() == 0)
                .forEach(FAKER_PROVIDERS::add);
    }

    public String randomKey(SerdeType serde) {
        return random(serde);
    }

    public String randomValue(SerdeType serde) {
        return random(serde);
    }

    public String random(SerdeType serde) {
        return switch (serde) {
            case JSON -> generateJson();
            case SHORT -> String.valueOf((short) RandomUtils.secure().randomInt());
            case INTEGER -> String.valueOf(RandomUtils.secure().randomInt());
            case LONG -> String.valueOf(RandomUtils.secure().randomLong());
            case FLOAT -> String.valueOf(RandomUtils.secure().randomFloat());
            case DOUBLE -> String.valueOf(RandomUtils.secure().randomDouble());
            case UUID -> UUID.randomUUID().toString();
            default -> RandomStringUtils.secure().nextAlphanumeric(1, 512);
        };
    }

    private String generateJson() {
        var providers = new ArrayList<Method>();
        for (int i = 0; i < RandomUtils.secure().randomInt(0, 24); i++) {
            providers.add(FAKER_PROVIDERS.get(RandomUtils.secure().randomInt(0, FAKER_PROVIDERS.size())));
        }
        var fields = providers.stream()
                .map(it -> {
                    try {
                        var value = generateJsonObject(it.invoke(FAKER));
                        return StringUtils.isBlank(value) ? null : "\"" + it.getName() + "\":" + value;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        return "{" + fields + "}";
    }

    private String generateJsonObject(Object object) {
        var fields = Arrays.stream(object.getClass().getDeclaredMethods())
                .filter(it -> Modifier.isPublic(it.getModifiers()) && it.getParameterCount() == 0)
                .map(it -> {
                    try {
                        var value = ClassUtils.getPackageName(it.getReturnType()).startsWith("net.datafaker.providers")
                                ? generateJsonObject(it.invoke(object))
                                : "\"" + it.invoke(object).toString().replace("\"", "'") + "\"";
                        return "\"" + it.getName() + "\":" + value;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        return StringUtils.isBlank(fields) ? null : "{" + fields + "}";
    }
}
