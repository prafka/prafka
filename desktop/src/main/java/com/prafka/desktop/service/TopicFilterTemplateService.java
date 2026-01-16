package com.prafka.desktop.service;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.prafka.core.model.ConsumeFilter;
import com.prafka.core.util.CollectionUtils;
import com.prafka.core.util.GsonOptionalAdapter;
import com.prafka.core.util.JsonFactory;
import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.TopicFilterTemplateModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

@Singleton
public class TopicFilterTemplateService {

    private static final Gson gsonImportExport = new GsonBuilder()
            .registerTypeAdapterFactory(GsonOptionalAdapter.FACTORY)
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .addSerializationExclusionStrategy(new TopicFilterTemplateService.ExportExclusionStrategy())
            .create();

    private final SessionService sessionService;
    private final StorageService storageService;

    @Inject
    public TopicFilterTemplateService(SessionService sessionService, StorageService storageService) {
        this.sessionService = sessionService;
        this.storageService = storageService;
    }

    public List<TopicFilterTemplateModel> getAll(String topicName) {
        return storageService.getEncryptedStorage()
                .getClusterTopicsFilterTemplates()
                .computeIfAbsent(sessionService.getCluster().getId(), key -> new HashMap<>())
                .computeIfAbsent(topicName, key -> new ArrayList<>());
    }

    public Optional<TopicFilterTemplateModel> getDefault(String topicName) {
        return getAll(topicName).stream().filter(TopicFilterTemplateModel::isByDefault).findFirst();
    }

    public void save(String topicName, TopicFilterTemplateModel template) {
        var existingTemplate = getAll(topicName).stream()
                .filter(it -> Strings.CS.equals(it.getId(), template.getId()))
                .findFirst();
        if (existingTemplate.isPresent()) {
            existingTemplate.get().setFilter(template.getFilter());
            existingTemplate.get().setUpdatedAt(Instant.now().toEpochMilli());
        } else {
            getAll(topicName).add(template);
        }
        storageService.saveEncryptedStorage();
    }

    public void saveDefault(String topicName, Optional<TopicFilterTemplateModel> templateOpt) {
        templateOpt.ifPresentOrElse(
                template -> getAll(topicName).forEach(it -> it.setByDefault(Strings.CS.equals(it.getId(), template.getId()))),
                () -> getAll(topicName).forEach(it -> it.setByDefault(false))
        );
        storageService.saveEncryptedStorage();
    }

    public void delete(String topicName, TopicFilterTemplateModel template) {
        getAll(topicName).removeIf(it -> Strings.CS.equals(it.getId(), template.getId()));
        storageService.saveEncryptedStorage();
    }

    public void importTemplates(String topicName, String json) {
        Supplier<IllegalArgumentException> error = () -> new IllegalArgumentException("invalid structure");
        var map = gsonImportExport.fromJson(json, JsonFactory.MAP_STING_OBJECT_TYPE.getType());
        var tree = gsonImportExport.toJsonTree(map).getAsJsonObject();
        if (tree.get("version") == null || tree.get("filters") == null) {
            throw error.get();
        }
        var templates = gsonImportExport.fromJson(tree.get("filters"), new TypeToken<Collection<TopicFilterTemplateModel>>() {
        });
        if (templates.size() < 1) {
            throw error.get();
        }
        for (TopicFilterTemplateModel template : templates) {
            var filter = template.getFilter();
            if (filter == null
                    || filter.from() == null
                    || filter.keySerde() == null
                    || filter.valueSerde() == null
            ) {
                throw error.get();
            }
            var expressions = filter.expressions();
            if (expressions != null) {
                for (ConsumeFilter.Expression expression : expressions) {
                    if (StringUtils.isBlank(expression.code())) {
                        throw error.get();
                    }
                }
            }
        }
        getAll(topicName).addAll(templates);
        storageService.saveEncryptedStorage();
    }

    public String exportTemplates(String topicName) {
        return gsonImportExport.toJson(CollectionUtils.mapOf("version", ApplicationProperties.VERSION, "filters", getAll(topicName)));
    }

    private static class ExportExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            if (Strings.CS.equalsAny(f.getName(), "id", "createdAt", "updatedAt")) {
                return true;
            }
            if (f.getDeclaringClass() == TopicFilterTemplateModel.class && Strings.CS.equalsAny(f.getName(), "byDefault")) {
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
