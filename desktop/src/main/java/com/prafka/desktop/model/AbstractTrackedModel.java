package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public abstract class AbstractTrackedModel {

    protected String id = UUID.randomUUID().toString();
    protected long createdAt = Instant.now().toEpochMilli();
    protected long updatedAt = Instant.now().toEpochMilli();
}
