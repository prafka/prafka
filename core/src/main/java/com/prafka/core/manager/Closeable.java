package com.prafka.core.manager;

/**
 * Extension of {@link java.io.Closeable} that supports closing resources for a specific cluster.
 *
 * <p>This interface allows resource managers to release connections and cached instances
 * either globally (via {@link #close()}) or for a specific cluster (via {@link #close(String)}).
 */
public interface Closeable extends java.io.Closeable {

    /**
     * Releases resources associated with a specific cluster.
     *
     * @param clusterId the unique identifier of the cluster whose resources should be released
     */
    void close(String clusterId);
}
