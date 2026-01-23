package com.prafka.core.model;

/**
 * Enumeration of serialization/deserialization types for Kafka record keys and values.
 *
 * <p>Defines the available serializers and deserializers that can be used when
 * producing or consuming Kafka messages.
 */
public enum SerdeType {

    /**
     * Represents a null value (tombstone for keys).
     */
    NULL,

    /**
     * Automatic type detection based on content.
     */
    AUTO,

    /**
     * Schema Registry-based serialization with schema ID lookup.
     */
    SCHEMA_REGISTRY,

    /**
     * JSON serialization/deserialization.
     */
    JSON,

    /**
     * Plain string serialization using UTF-8 encoding.
     */
    STRING,

    /**
     * Raw byte array serialization.
     */
    BYTES,

    /**
     * 16-bit signed integer serialization.
     */
    SHORT,

    /**
     * 32-bit signed integer serialization.
     */
    INTEGER,

    /**
     * 64-bit signed integer serialization.
     */
    LONG,

    /**
     * 32-bit floating point serialization.
     */
    FLOAT,

    /**
     * 64-bit floating point serialization.
     */
    DOUBLE,

    /**
     * UUID string serialization.
     */
    UUID,

    /**
     * Apache Avro binary serialization.
     */
    AVRO,
}
