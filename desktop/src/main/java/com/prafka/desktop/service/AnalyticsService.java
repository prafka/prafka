package com.prafka.desktop.service;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.concurrent.ScheduledServiceAdapter;
import com.prafka.desktop.model.AnalyticsModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.util.Duration;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static com.prafka.core.util.CollectionUtils.mapOf;

/**
 * Collects and sends application analytics data to the backend server.
 *
 * <p>Manages a queue of analytics events and periodically batches them
 * for transmission. Respects user privacy settings for analytics collection.
 */
@Singleton
public class AnalyticsService {

    private static final int BATCH_SIZE = 10;
    private static final Queue<AnalyticsModel> queue = new ArrayBlockingQueue<>(100);

    private final StorageService storageService;
    private final BackendClient backendClient;

    @Inject
    public AnalyticsService(StorageService storageService, BackendClient backendClient) {
        this.storageService = storageService;
        this.backendClient = backendClient;
        ScheduledServiceAdapter.scheduleTask(this::sendAnalytics).start(Duration.seconds(10), Duration.seconds(10));
    }

    private void sendAnalytics() {
        var models = new ArrayList<AnalyticsModel>();
        while (models.size() < BATCH_SIZE && !queue.isEmpty()) {
            models.add(queue.poll());
        }
        if (!models.isEmpty()) backendClient.analytics(models).join();
    }

    private boolean disabled() {
        return !storageService.getPlainStorage().isCollectAnalytics();
    }

    private String userId() {
        return storageService.getPlainStorage().getUserId();
    }

    public void collectAnalytics(boolean value) {
        storageService.getPlainStorage().setCollectAnalytics(value);
        storageService.savePlainStorage();
    }

    public void uncaughtException(Throwable throwable) {
        if (disabled()) return;
        queue.offer(new AnalyticsModel(ApplicationProperties.VERSION, userId(), AnalyticsModel.Type.UNCAUGHT_EXCEPTION, mapOf("throwable", ExceptionUtils.getStackTrace(throwable))));
    }
}
