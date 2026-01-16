package com.prafka.core.manager;

public interface Closeable extends java.io.Closeable {

    void close(String clusterId);
}
