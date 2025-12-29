package com.github.sinwe.gradle.caching.hazelcast;

import org.gradle.caching.configuration.AbstractBuildCache;

/**
 * Build cache configuration for Hazelcast backends. Pushing to this cache is enabled by default.
 *
 * <p>Configuration via {@code settings.gradle}:</p>
 *
 * <pre>
 * buildCache {
 *     local {
 *         // Disable local cache, as Hazelcast will serve as both local and remote
 *         enabled = false
 *     }
 *     remote(com.github.sinwe.hazelcast.HazelcastBuildCache) {
 *         // ...
 *     }
 * }
 * </pre>
 */
public class HazelcastBuildCache extends AbstractBuildCache {

    private String name;
    private String host;
    private int port;

    public HazelcastBuildCache() {
        this.host = System.getProperty("com.github.sinwe.gradle.caching.hazelcast.host", "127.0.0.1");
        this.port = getPortValue();
        this.name = System.getProperty("com.github.sinwe.gradle.caching.hazelcast.name", "gradle-task-cache");
    }

    private static int getPortValue() {
        String portString = System.getProperty("com.github.sinwe.gradle.caching.hazelcast.port", "5701");
        int portValue;
        try {
            portValue = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            portValue = 5701;
        }
        return portValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("cache name must not be empty");
        }
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("host name must not be empty");
        }
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("port must be a positive number");
        }
        this.port = port;
    }
}
