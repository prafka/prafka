package com.prafka.desktop.service;

import jakarta.inject.Singleton;
import javafx.scene.Node;
import org.apache.commons.lang3.event.EventListenerSupport;

/**
 * Event bus for dashboard navigation and content updates.
 *
 * <p>Provides a publish-subscribe mechanism for dashboard-related events,
 * allowing components to communicate without direct coupling.
 */
@Singleton
public class EventService {

    private static final EventListenerSupport<DashboardListener> DASHBOARD = EventListenerSupport.create(DashboardListener.class);

    public void register(DashboardListener listener) {
        DASHBOARD.addListener(listener);
    }

    public void fire(DashboardEvent event) {
        DASHBOARD.fire().onEvent(event);
    }

    public void fire(DashboardContentEvent event) {
        DASHBOARD.fire().onEvent(event);
    }

    public interface DashboardListener {

        void onEvent(DashboardEvent event);

        void onEvent(DashboardContentEvent event);
    }

    public enum DashboardEvent {
        UPDATE_COMBOBOX_CLUSTERS,
        UPDATE_COMBOBOX_CLUSTERS_SILENT,
        LOAD_OVERVIEW,
        LOAD_TOPICS,
        LOAD_SCHEMA_REGISTRY,
        LOAD_CONSUMER_GROUPS,
        LOAD_KAFKA_CONNECT,
        LOAD_BROKERS,
        LOAD_ACL,
        LOAD_QUOTAS,
        LOAD_CLUSTERS,
//        LOAD_CERTIFICATES,
    }

    public record DashboardContentEvent(Node content) {
    }
}
