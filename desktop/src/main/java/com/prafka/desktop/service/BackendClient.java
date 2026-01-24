package com.prafka.desktop.service;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.model.AnalyticsModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.prafka.core.util.JsonFactory.MAP_STING_STRING_TYPE;
import static com.prafka.core.util.JsonFactory.gsonDefault;

/**
 * HTTP client for communication with the backend API.
 *
 * <p>Provides methods for retrieving application version information
 * and submitting analytics data.
 */
@Singleton
public class BackendClient {

    private static final String contentType = "Content-Type";
    private static final String applicationJson = "application/json";

    private final String baseUrl;

    @Inject
    public BackendClient(ApplicationProperties applicationProperties) {
        this.baseUrl = applicationProperties.apiUrl();
    }

    public CompletableFuture<String> getCurrentVersion() {
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(baseUrl + "/current-version"))
                .header(contentType, applicationJson)
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return gsonDefault.fromJson(response.body(), MAP_STING_STRING_TYPE).get("version");
                    } else {
                        throw new IllegalStateException();
                    }
                });
    }

    public CompletableFuture<String> analytics(List<AnalyticsModel> body) {
        var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(gsonDefault.toJson(body)))
                .uri(URI.create(baseUrl + "/analytics"))
                .header(contentType, applicationJson)
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .build();
        return HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        throw new IllegalStateException();
                    }
                });
    }
}
