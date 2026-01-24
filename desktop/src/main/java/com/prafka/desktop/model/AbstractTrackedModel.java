package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for models that require unique identification and timestamp tracking.
 *
 * <p>Provides auto-generated UUID and creation/update timestamps for all extending models.
 */
@Getter
@Setter
public abstract class AbstractTrackedModel {

    protected String id = UUID.randomUUID().toString();
    protected long createdAt = Instant.now().toEpochMilli();
    protected long updatedAt = Instant.now().toEpochMilli();
}
