package com.github.sinwe.gradle.caching.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;

import java.util.logging.Level;

public class HazelcastService {

    private final int port;
    private HazelcastInstance instance;

    public HazelcastService() {
        this(5701);
    }

    public HazelcastService(int port) {
        this.port = port;
    }

    public void start() {
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

    public void stop() {
        if (instance != null) {
            instance.shutdown();
        }
    }
}
