package com.prafka.core.service;

import com.google.gson.reflect.TypeToken;
import com.prafka.core.model.Connector;
import com.prafka.core.util.JsonFactory;
import com.prafka.core.util.StreamUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;
import org.sourcelab.kafka.connect.apiclient.request.dto.NewConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.request.post.PostConnectorRestart;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service for managing Kafka Connect connectors.
 *
 * <p>Provides operations to list, create, update, delete, pause, resume, and restart
 * connectors and their tasks. Also supports configuration validation.
 *
 * @see Connector
 */
@Named
@Singleton
public class ConnectService extends AbstractService {

    public CompletableFuture<List<Connector.Name>> getAllNames(String clusterId) {
        return supplyAsync(() -> connectClients(clusterId))
                .thenCompose(clients -> clients.entrySet().stream()
                        .map(entry -> supplyAsync(() -> entry.getValue().getConnectors().stream().map(it -> new Connector.Name(entry.getKey(), it)).toList())
                                .exceptionally(it -> Collections.emptyList()))
                        .reduce(StreamUtils.combineFutureList())
                        .orElseGet(StreamUtils.completeFutureEmptyList()));
    }

    public CompletableFuture<List<Connector.Name>> getAllNames(String clusterId, String connectId) {
        return supplyAsync(() -> connectClient(clusterId, connectId).getConnectors().stream().map(name -> new Connector.Name(connectId, name)).toList());
    }

    public CompletableFuture<Connector> get(String clusterId, Connector.Name cn) {
        return supplyAsync(() -> {
            var definition = connectClient(clusterId, cn.connectId()).getConnector(cn.name());
            var status = connectClient(clusterId, cn.connectId()).getConnectorStatus(cn.name());
            var topics = connectClient(clusterId, cn.connectId()).getConnectorTopics(cn.name());
            return new Connector(definition, status, topics);
        });
    }

    public CompletableFuture<Void> pause(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).pauseConnector(cn.name()));
    }

    public CompletableFuture<Void> resume(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).resumeConnector(cn.name()));
    }

    public CompletableFuture<Void> restart(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).restartConnector(cn.name()));
    }

    public CompletableFuture<Void> restartAllTasks(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).restartConnector(new PostConnectorRestart(cn.name()).withIncludeTasks(true)));
    }

    public CompletableFuture<Void> restartFailedTasks(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).restartConnector(new PostConnectorRestart(cn.name()).withOnlyFailed(true)));
    }

    public CompletableFuture<Void> restartTask(String clusterId, Connector.Name cn, int taskId) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).restartConnectorTask(cn.name(), taskId));
    }

    public CompletableFuture<Void> delete(String clusterId, Connector.Name cn) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).deleteConnector(cn.name()));
    }

    public CompletableFuture<Void> validate(String clusterId, String connectId, String plugin, Map<String, String> config) {
        return runAsync(() -> {
            var definition = ConnectorPluginConfigDefinition.newBuilder().withName(plugin).withConfig(config).build();
            var result = connectClient(clusterId, connectId).validateConnectorPluginConfig(definition);
            if (result.getErrorCount() > 0) throw new ConnectorValidateError(result);
        });
    }

    public CompletableFuture<Void> create(String clusterId, String connectId, String name, Map<String, String> config) {
        return runAsync(() -> {
            var definition = NewConnectorDefinition.newBuilder().withName(name).withConfig(config).build();
            connectClient(clusterId, connectId).addConnector(definition);
        });
    }

    public CompletableFuture<Void> update(String clusterId, Connector.Name cn, Map<String, String> config) {
        return runAsync(() -> connectClient(clusterId, cn.connectId()).updateConnectorConfig(cn.name(), config));
    }

    public CompletableFuture<Void> delete(String clusterId, String connectId, String name) {
        return runAsync(() -> connectClient(clusterId, connectId).deleteConnector(name));
    }

    public record AllConnectorsSummary(long connectorCount, long runConnectorCount, long failConnectorCount,
                                       long runTaskCount, long failTaskCount) {
    }

    public CompletableFuture<AllConnectorsSummary> getAllConnectorsSummary(String clusterId) {
        return supplyAsync(() -> connectClients(clusterId))
                .thenCompose(clients -> clients.values().stream()
                        .map(connectClient -> supplyAsync(() -> connectClient.getConnectorsWithExpandedStatus().getAllStatuses().stream().toList())
                                .exceptionally(it -> Collections.emptyList()))
                        .reduce(StreamUtils.combineFutureList())
                        .orElseGet(StreamUtils.completeFutureEmptyList())
                        .thenApply(list -> new AllConnectorsSummary(
                                list.size(),
                                list.stream().filter(it -> Connector.State.valueOf(it.getConnector().get("state")) == Connector.State.RUNNING).count(),
                                list.stream().filter(it -> Connector.State.valueOf(it.getConnector().get("state")) == Connector.State.FAILED).count(),
                                list.stream().flatMap(it -> it.getTasks().stream()).filter(it -> Connector.Task.State.valueOf(it.getState()) == Connector.Task.State.RUNNING).count(),
                                list.stream().flatMap(it -> it.getTasks().stream()).filter(it -> Connector.Task.State.valueOf(it.getState()) == Connector.Task.State.FAILED).count()
                        )));
    }

    public record ConnectorSummary(Connector.Type type, Connector.State state, Connector.Plugin plugin, String workerId,
                                   long runTaskCount, long failTaskCount) {
    }

    public CompletableFuture<ConnectorSummary> getConnectorSummary(String clusterId, Connector.Name cn) {
        return get(clusterId, cn)
                .thenApply(connector -> new ConnectorSummary(
                        connector.getType(),
                        connector.getState(),
                        connector.getPlugin(),
                        connector.getWorkerId(),
                        connector.getTasks().stream().filter(it -> it.getState() == Connector.Task.State.RUNNING).count(),
                        connector.getTasks().stream().filter(it -> it.getState() == Connector.Task.State.FAILED).count()
                ));
    }

    @Getter
    public static class ConnectorValidateError extends RuntimeException {

        private static final TypeToken<List<Config>> TYPE = new TypeToken<>() {
        };

        private final List<Config> configs;

        public ConnectorValidateError(ConnectorPluginConfigValidationResults result) {
            configs = JsonFactory.gsonDefault.fromJson(JsonFactory.gsonDefault.toJson(result.getConfigs()), TYPE);
        }

        public record Config(Map<String, Object> definition, Map<String, Object> value) {
        }
    }

    private static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ExecutorHolder.connectExecutor);
    }

    private static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ExecutorHolder.connectExecutor);
    }
}
