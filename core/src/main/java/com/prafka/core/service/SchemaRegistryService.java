package com.prafka.core.service;

import com.prafka.core.model.Schema;
import com.prafka.core.util.StreamUtils;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Named
@Singleton
public class SchemaRegistryService extends AbstractService {

    public CompletableFuture<Collection<String>> getAllSubjects(String clusterId) {
        return supplyAsync(() -> StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getAllSubjects()));
    }

    public CompletableFuture<Schema> get(String clusterId, String subject) {
        return get(clusterId, subject, Optional.empty());
    }

    public CompletableFuture<Schema> get(String clusterId, String subject, int version) {
        return get(clusterId, subject, Optional.of(version));
    }

    private CompletableFuture<Schema> get(String clusterId, String subject, Optional<Integer> version) {
        return supplyAsync(() -> {
            Schema.Compatibility compatibility;
            try {
                compatibility = new Schema.Compatibility(schemaRegistryClient(clusterId).getCompatibility(subject), false);
            } catch (RestClientException e) {
                if (e.getStatus() == 404) {
                    compatibility = new Schema.Compatibility(StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getCompatibility(null)), true);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            var metadata = version
                    .map(it -> StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getSchemaMetadata(subject, it)))
                    .orElseGet(() -> StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getLatestSchemaMetadata(subject)));
            var versions = StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getAllVersions(subject));
            var parsed = StreamUtils.tryReturn(() -> schemaRegistryClient(clusterId).getSchemaBySubjectAndId(subject, metadata.getId()));
            return new Schema(subject, compatibility, metadata, versions, parsed);
        });
    }

    public CompletableFuture<Void> create(String clusterId, String subject, Schema.Type type, String raw) {
        return runAsync(() -> StreamUtils.tryVoid(() ->
                schemaRegistryClient(clusterId).register(subject, rawToParsed(type, raw), true)
        ));
    }

    public CompletableFuture<Void> updateCompatibility(String clusterId, String subject, CompatibilityLevel compatibility) {
        return runAsync(() -> StreamUtils.tryVoid(() ->
                schemaRegistryClient(clusterId).updateCompatibility(subject, compatibility.name())
        ));
    }

    public CompletableFuture<Void> checkCompatibility(String clusterId, String subject, Schema.Type type, String raw) {
        return runAsync(() -> StreamUtils.tryVoid(() -> {
            var result = schemaRegistryClient(clusterId).testCompatibilityVerbose(subject, rawToParsed(type, raw));
            if (CollectionUtils.isNotEmpty(result)) throw new SchemaNotCompatibleException(result);
        }));
    }

    public CompletableFuture<Void> update(String clusterId, String subject, Schema.Type type, String raw) {
        return runAsync(() -> StreamUtils.tryVoid(() ->
                schemaRegistryClient(clusterId).register(subject, rawToParsed(type, raw), true)
        ));
    }

    public CompletableFuture<Void> delete(String clusterId, String subject, boolean permanently) {
        return runAsync(() -> StreamUtils.tryVoid(() -> {
            schemaRegistryClient(clusterId).deleteSubject(subject);
            if (permanently) schemaRegistryClient(clusterId).deleteSubject(subject, true);
        }));
    }

    public CompletableFuture<Void> delete(String clusterId, String subject, int version, boolean permanently) {
        return runAsync(() -> StreamUtils.tryVoid(() -> {
            schemaRegistryClient(clusterId).deleteSchemaVersion(subject, String.valueOf(version));
            if (permanently)
                schemaRegistryClient(clusterId).deleteSchemaVersion(subject, String.valueOf(version), true);
        }));
    }

    private static ParsedSchema rawToParsed(Schema.Type type, String raw) {
        return switch (type) {
            case AVRO -> new AvroSchema(raw);
            case JSON -> new JsonSchema(raw);
            case PROTOBUF -> new ProtobufSchema(raw);
        };
    }

    public record AllSchemasSummary(int count, int countDeleted, String compatibility, String mode) {
    }

    public CompletableFuture<AllSchemasSummary> getAllSchemasSummary(String clusterId) {
        return supplyAsync(() -> StreamUtils.tryReturn(() -> {
            var count = schemaRegistryClient(clusterId).getAllSubjects().size();
            var countWithDeleted = schemaRegistryClient(clusterId).getAllSubjects(true).size();
            var compatibility = schemaRegistryClient(clusterId).getCompatibility(null);
            var mode = schemaRegistryClient(clusterId).getMode();
            return new AllSchemasSummary(count, countWithDeleted - count, compatibility, mode);
        }));
    }

    @Getter
    @RequiredArgsConstructor
    public static class SchemaNotCompatibleException extends RuntimeException {

        private final List<String> errors;
    }

    private static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ExecutorHolder.schemaRegistryExecutor);
    }

    private static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ExecutorHolder.schemaRegistryExecutor);
    }
}
