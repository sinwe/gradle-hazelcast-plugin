package org.gradle.caching.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.junit.rules.ExternalResource;

import java.util.logging.Level;

public class HazelcastService extends ExternalResource {

    private final int port;
    private HazelcastInstance instance;

    public HazelcastService() {
        this(5701);
    }

    public HazelcastService(int port) {
        this.port = port;
    }

    @Override
    protected void before() throws Throwable {
        java.util.logging.Logger.getLogger("com.hazelcast").setLevel(Level.WARNING);
        Config config = new Config();
        config
            .getNetworkConfig()
            .setPort(port)
            .getJoin()
            .getMulticastConfig()
            .setEnabled(false);
        instance = Hazelcast.newHazelcastInstance(config);
    }

    public int getPort() {
        return port;
    }

    @Override
    protected void after() {
        instance.shutdown();
    }
}
