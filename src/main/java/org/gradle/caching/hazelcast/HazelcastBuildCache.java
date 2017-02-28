package org.gradle.caching.hazelcast;

import com.hazelcast.util.Preconditions;
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
 *     remote(org.gradle.caching.hazelcast.HazelcastBuildCache) {
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
        this.host = System.getProperty("org.gradle.caching.hazelcast.host", "127.0.0.1");
        this.port = getPortValue();
        this.name = System.getProperty("org.gradle.caching.hazelcast.name", "gradle-task-cache");
        // Allow pushing by default
        setPush(true);
    }

    private static int getPortValue() {
        String portString = System.getProperty("org.gradle.caching.hazelcast.port", "5701");
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
        Preconditions.checkHasText(name, "cache name must not be empty");
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        Preconditions.checkHasText(host, "host name must not be empty");
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        Preconditions.checkPositive(port, "port must be a positive number");
        this.port = port;
    }
}
