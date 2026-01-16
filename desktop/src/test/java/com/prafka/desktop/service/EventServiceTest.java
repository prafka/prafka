package com.prafka.desktop.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {

    @Test
    void shouldFire() {
        // Given
        var actualEvent = new AtomicReference<EventService.DashboardEvent>();

        var eventService = new EventService();
        eventService.register(new EventService.DashboardListener() {
            @Override
            public void onEvent(EventService.DashboardEvent event) {
                actualEvent.set(event);
            }
            @Override
            public void onEvent(EventService.DashboardContentEvent event) {
            }
        });

        // When
        eventService.fire(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS);

        // Then
        assertEquals(EventService.DashboardEvent.UPDATE_COMBOBOX_CLUSTERS, actualEvent.get());
    }
}