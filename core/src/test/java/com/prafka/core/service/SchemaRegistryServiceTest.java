package com.prafka.core.service;

import com.prafka.core.model.Schema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaRegistryServiceTest {

    private SchemaRegistryClient schemaRegistryClient = mock(SchemaRegistryClient.class);
    private SchemaRegistryService schemaRegistryService = new SchemaRegistryService() {
        @Override
        protected SchemaRegistryClient schemaRegistryClient(String clusterId) {
            return schemaRegistryClient;
        }
    };

    @Test
    void shouldGet() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var subject = "test-subject";
        int version = 2;
        var compatibility = "BACKWARD";
        int schemaId = 1;
        var schemaString = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";

        var metadata = new SchemaMetadata(schemaId, version, schemaString);
        var versions = List.of(1, 2);
        var parsedSchema = new AvroSchema(schemaString);

        when(schemaRegistryClient.getCompatibility(subject)).thenReturn(compatibility);
        when(schemaRegistryClient.getSchemaMetadata(subject, version)).thenReturn(metadata);
        when(schemaRegistryClient.getAllVersions(subject)).thenReturn(versions);
        when(schemaRegistryClient.getSchemaBySubjectAndId(subject, schemaId)).thenReturn(parsedSchema);

        // When
        var result = schemaRegistryService.get(clusterId, subject, version).get();

        // Then
        assertEquals(subject, result.getSubject());
        assertEquals(compatibility, result.getCompatibility().getLevel().name());
        assertEquals(schemaId, result.getId());
        assertEquals(version, result.getVersion());
        assertEquals(schemaString, result.getRaw());
        assertEquals(versions, result.getVersions());
    }

    @Test
    void shouldGetAllSchemasSummary() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var allSubjects = List.of("subject1", "subject2", "subject3");
        var allSubjectsWithDeleted = List.of("subject1", "subject2", "subject3", "deletedSubject");
        var compatibility = "BACKWARD";
        var mode = "READWRITE";

        when(schemaRegistryClient.getAllSubjects()).thenReturn(allSubjects);
        when(schemaRegistryClient.getAllSubjects(true)).thenReturn(allSubjectsWithDeleted);
        when(schemaRegistryClient.getCompatibility(null)).thenReturn(compatibility);
        when(schemaRegistryClient.getMode()).thenReturn(mode);

        // When
        var result = schemaRegistryService.getAllSchemasSummary(clusterId).get();

        // Then
        assertEquals(3, result.count());
        assertEquals(1, result.countDeleted());
        assertEquals(compatibility, result.compatibility());
        assertEquals(mode, result.mode());
    }

    @Test
    void shouldCreateSchema() throws Exception {
        // Given
        var clusterId = "test-cluster";
        var subject = "test-subject";
        var type = Schema.Type.AVRO;
        var schemaString = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";

        // When
        schemaRegistryService.create(clusterId, subject, type, schemaString).get();

        // Then
        verify(schemaRegistryClient).register(subject, new AvroSchema(schemaString), true);
    }
}