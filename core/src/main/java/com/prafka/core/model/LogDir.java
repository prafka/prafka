package com.prafka.core.model;

import lombok.Getter;
import org.apache.kafka.clients.admin.ReplicaInfo;

@Getter
public class LogDir {

    private final int brokerId;
    private final String path;
    private final long size;
    private final long offsetLag;

    public LogDir(int brokerId, String path, ReplicaInfo replicaInfo) {
        this.brokerId = brokerId;
        this.path = path;
        size = replicaInfo.size();
        offsetLag = replicaInfo.offsetLag();
    }
}
