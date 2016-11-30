package org.gradle.cache.tasks.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.cache.BuildCache;
import org.gradle.cache.BuildCacheFactory;
import org.gradle.cache.MapBasedBuildCache;

public class HazelcastPlugin implements Plugin<Gradle> {
    @Override
    public void apply(Gradle gradle) {
        ((GradleInternal) gradle).getBuildCache().useCacheFactory(new BuildCacheFactory() {
            @Override
            public BuildCache createCache(StartParameter startParameter) {
                ClientConfig config = new ClientConfig();
                String host = System.getProperty("org.gradle.cache.tasks.hazelcast.host", "127.0.0.1");
                String port = System.getProperty("org.gradle.cache.tasks.hazelcast.port", "5701");
                String name = System.getProperty("org.gradle.cache.tasks.hazelcast.name", "gradle-task-cache");
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
