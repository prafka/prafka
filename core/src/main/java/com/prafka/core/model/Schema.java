package com.prafka.core.model;

import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import lombok.Getter;
import org.everit.json.schema.ObjectSchema;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a schema registered in Confluent Schema Registry.
 *
 * <p>Supports Avro, JSON Schema, and Protobuf schema types. Contains schema metadata
 * including the subject, version history, compatibility level, raw schema definition,
 * and parsed field information.
 *
 * @see SchemaMetadata
 * @see ParsedSchema
 */
@Getter
public class Schema {

    private final int id;
    private final String subject;
    private final String record;
    private final Type type;
    private final Compatibility compatibility;
    private final int version;
    private final List<Integer> versions;
    private final String raw;
    private final List<Field> fields;

    public Schema(String subject, Compatibility compatibility, SchemaMetadata metadata, List<Integer> versions, ParsedSchema parsed) {
        id = metadata.getId();
        this.subject = subject;
        record = parsed.name();
        type = Type.valueOf(metadata.getSchemaType());
        this.compatibility = compatibility;
        version = metadata.getVersion();
        this.versions = versions;
        raw = parsed.canonicalString();
        if (parsed.rawSchema() instanceof org.apache.avro.Schema source) {
            fields = mapAvroFields(source);
        } else if (parsed.rawSchema() instanceof com.squareup.wire.schema.internal.parser.ProtoFileElement source) {
            if (source.getTypes().size() == 1) {
                fields = mapProtobufFields(source.getTypes().getFirst());
            } else {
                fields = Collections.emptyList();
            }
        } else if (parsed.rawSchema() instanceof ObjectSchema source) {
            fields = mapJsonFields(source);
        } else {
            fields = Collections.emptyList();
        }
    }

    private List<Field> mapAvroFields(org.apache.avro.Schema source) {
        return source.getFields().stream().map(this::mapAvroField).toList();
    }

    private Field mapAvroField(org.apache.avro.Schema.Field source) {
        List<String> types;
        List<Field> fields;
        if (source.schema().getType() == org.apache.avro.Schema.Type.UNION) {
            types = source.schema().getTypes().stream().map(it -> it.getType().getName()).toList();
            fields = source.schema().getTypes().stream()
                    .filter(it -> it.getType() == org.apache.avro.Schema.Type.RECORD)
                    .findFirst()
                    .map(it -> it.getFields().stream().map(this::mapAvroField).toList())
                    .orElseGet(Collections::emptyList);
        } else {
            types = Collections.singletonList(source.schema().getType().getName());
            if (source.schema().getType() == org.apache.avro.Schema.Type.RECORD) {
                fields = source.schema().getFields().stream().map(this::mapAvroField).toList();
            } else {
                fields = Collections.emptyList();
            }
        }
        return new Field(source.name(), types, fields);
    }

    private List<Field> mapProtobufFields(com.squareup.wire.schema.internal.parser.TypeElement source) {
        if (source instanceof com.squareup.wire.schema.internal.parser.MessageElement messageElement) {
            return messageElement.getFields().stream().map(this::mapProtobufField).toList();
        } else {
            return Collections.emptyList();
        }
    }

    private Field mapProtobufField(com.squareup.wire.schema.internal.parser.FieldElement source) {
        return new Field(source.getName(), Collections.singletonList(source.getType()), Collections.emptyList());
    }

    private List<Field> mapJsonFields(ObjectSchema source) {
        Function<org.everit.json.schema.Schema, String> type = schema -> schema.getClass().getSimpleName().toLowerCase().replace("schema", "");
        return source.getPropertySchemas().entrySet().stream()
                .map(entry -> {
                    List<String> types;
                    List<Field> fields;
                    if (entry.getValue() instanceof org.everit.json.schema.CombinedSchema combinedSchema) {
                        types = combinedSchema.getSubschemas().stream().map(type).toList();
                        fields = combinedSchema.getSubschemas().stream()
                                .filter(it -> it instanceof ObjectSchema)
                                .findFirst()
                                .map(it -> mapJsonFields((ObjectSchema) it))
                                .orElseGet(Collections::emptyList);
                    } else {
                        types = Collections.singletonList(type.apply(entry.getValue()));
                        if (entry.getValue() instanceof ObjectSchema objectSchema) {
                            fields = mapJsonFields(objectSchema);
                        } else {
                            fields = Collections.emptyList();
                        }
                    }
                    return new Field(entry.getKey(), types, fields);
                })
                .toList();
    }

    public enum Type {
        AVRO,
        JSON,
        PROTOBUF,
    }

    @Getter
    public static class Compatibility {

        private final CompatibilityLevel level;
        private final boolean global;

        public Compatibility(String level, boolean global) {
            this.level = CompatibilityLevel.forName(level);
            this.global = global;
        }
    }

    public record Field(String name, List<String> types, List<Field> fields) {
    }
}
