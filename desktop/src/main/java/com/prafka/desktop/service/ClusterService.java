package com.prafka.desktop.service;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.prafka.core.util.CollectionUtils;
import com.prafka.core.util.GsonOptionalAdapter;
import com.prafka.core.util.JsonFactory;
import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.ClusterModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manages Kafka cluster configurations including CRUD operations and import/export.
 *
 * <p>Provides functionality to save, delete, and retrieve cluster configurations
 * from encrypted storage, as well as import and export cluster data in JSON format.
 */
@Singleton
public class ClusterService {

    private static final Gson gsonImportExport = new GsonBuilder()
            .registerTypeAdapterFactory(GsonOptionalAdapter.FACTORY)
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .addSerializationExclusionStrategy(new ExportExclusionStrategy())
            .create();

    private final StorageService storageService;

    @Inject
    public ClusterService(StorageService storageService) {
        this.storageService = storageService;
    }

    public List<ClusterModel> getClusters() {
        return storageService.getEncryptedStorage().getClusters();
    }

    public void saveCluster(ClusterModel cluster) {
        storageService.getEncryptedStorage().getClusters().removeIf(it -> Strings.CS.equals(it.getId(), cluster.getId()));
        storageService.getEncryptedStorage().getClusters().add(cluster);
        storageService.saveEncryptedStorage();
    }

    public void saveCurrentCluster(ClusterModel cluster) {
        storageService.getEncryptedStorage().getClusters().forEach(it -> it.setCurrent(false));
        storageService.getEncryptedStorage().getClusters().stream()
                .filter(it -> Strings.CS.equals(it.getId(), cluster.getId()))
                .findFirst()
                .orElseThrow()
                .setCurrent(true);
        storageService.saveEncryptedStorage();
    }

    public void deleteCluster(ClusterModel cluster) {
        storageService.getEncryptedStorage().getClusters().removeIf(it -> Strings.CS.equals(it.getId(), cluster.getId()));
        storageService.saveEncryptedStorage();
    }

    public void importClusters(String json) {
        Supplier<IllegalArgumentException> error = () -> new IllegalArgumentException("invalid structure");
        var map = gsonImportExport.fromJson(json, JsonFactory.MAP_STING_OBJECT_TYPE.getType());
        var tree = gsonImportExport.toJsonTree(map).getAsJsonObject();
        if (tree.get("version") == null || tree.get("clusters") == null) {
            throw error.get();
        }
        var clusters = gsonImportExport.fromJson(tree.get("clusters"), new TypeToken<Collection<ClusterModel>>() {
        });
        if (clusters.size() < 1) {
            throw error.get();
        }
        for (ClusterModel cluster : clusters) {
            if (StringUtils.isBlank(cluster.getName())
                    || StringUtils.isBlank(cluster.getBootstrapServers())
                    || cluster.getAuthenticationMethod() == null) {
                throw error.get();
            }
            var schema = cluster.getSchemaRegistry();
            if (schema != null) {
                if (StringUtils.isBlank(schema.getUrl())
                        || schema.getAuthenticationMethod() == null) {
                    throw error.get();
                }
            }
            var connects = cluster.getConnects();
            if (connects != null) {
                for (ClusterModel.ConnectModel connect : connects) {
                    if (StringUtils.isBlank(connect.getName())
                            || StringUtils.isBlank(connect.getUrl())) {
                        throw error.get();
                    }
                }
            }
        }
        storageService.getEncryptedStorage().getClusters().addAll(clusters);
        storageService.saveEncryptedStorage();
    }

    public String exportClusters() {
        return gsonImportExport.toJson(CollectionUtils.mapOf("version", ApplicationProperties.VERSION, "clusters", getClusters()));
    }

    private static class ExportExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            if (Strings.CS.equalsAny(f.getName(), "id", "createdAt", "updatedAt")) {
                return true;
            }
            if (f.getDeclaringClass() == ClusterModel.class && Strings.CS.equalsAny(f.getName(), "current")) {
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
