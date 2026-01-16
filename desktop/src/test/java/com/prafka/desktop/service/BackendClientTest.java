package com.prafka.desktop.service;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.AnalyticsModel;
import javafx.application.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackendClientTest {

    private Application application = mock(Application.class, RETURNS_DEEP_STUBS);
    private BackendClient backendClient = new BackendClient(new ApplicationProperties(application));

    @BeforeEach
    void setUp() {
        when(application.getParameters().getNamed()).thenReturn(Map.of());
    }

    @Test
    void shouldGetCurrentVersion() throws Exception {
        try (var mockStaticHttpClient = mockStatic(HttpClient.class)) {
            // Given
            var httpClient = mock(HttpClient.class);
            mockStaticHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            var response = "{\"version\": \"1.0.0\"}";
            HttpResponse<String> httpResponse = mock(HttpResponse.class);

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(response);
            when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(CompletableFuture.completedFuture(httpResponse));

            // When
            var result = backendClient.getCurrentVersion().get();

            // Then
            assertEquals("1.0.0", result);
        }
    }

    @Test
    void shouldAnalytics() throws Exception {
        try (var mockStaticHttpClient = mockStatic(HttpClient.class)) {
            // Given
            var httpClient = mock(HttpClient.class);
            mockStaticHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            var response = "success";
            HttpResponse<String> httpResponse = mock(HttpResponse.class);

            when(httpResponse.statusCode()).thenReturn(200);
            when(httpResponse.body()).thenReturn(response);
            when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(CompletableFuture.completedFuture(httpResponse));

            // When
            var analyticsModel = new AnalyticsModel("1.0.0", UUID.randomUUID().toString(), AnalyticsModel.Type.GET_VIEW, Map.of("view", "TopicView"));
            var result = backendClient.analytics(List.of(analyticsModel)).get();

            // Then
            assertEquals(response, result);
        }
    }
}