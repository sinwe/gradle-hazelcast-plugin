package org.gradle.caching.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.caching.BuildCache;
import org.gradle.caching.MapBasedBuildCache;
import org.gradle.caching.internal.BuildCacheFactory;

public class HazelcastPlugin implements Plugin<Gradle> {
    @Override
    public void apply(Gradle gradle) {
        ((GradleInternal) gradle).getBuildCache().useCacheFactory(new BuildCacheFactory() {
            @Override
            public BuildCache createCache(StartParameter startParameter) {
                ClientConfig config = new ClientConfig();
                String host = System.getProperty("org.gradle.caching.hazelcast.host", "127.0.0.1");
                String port = System.getProperty("org.gradle.caching.hazelcast.port", "5701");
                String name = System.getProperty("org.gradle.caching.hazelcast.name", "gradle-task-cache");
                String address = host + ":" + port;
                config.getNetworkConfig().addAddress(address);
                final HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);
                return new MapBasedBuildCache(
                    String.format("Hazelcast cache '%s' at %s", name, address),
                    instance.<String, byte[]>getMap(name)
                );
            }
        });
    }
}
