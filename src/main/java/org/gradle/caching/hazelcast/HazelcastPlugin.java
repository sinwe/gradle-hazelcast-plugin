package org.gradle.caching.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;
import org.gradle.caching.MapBasedBuildCacheService;

public class HazelcastPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        settings.getBuildCache().registerBuildCacheServiceFactory(new BuildCacheServiceFactory<HazelcastBuildCache>() {
            @Override
            public Class<HazelcastBuildCache> getConfigurationType() {
                return HazelcastBuildCache.class;
            }

            @Override
            public BuildCacheService build(HazelcastBuildCache cacheConfig) {
                ClientConfig config = new ClientConfig();
                String address = cacheConfig.getHost() + ":" + cacheConfig.getPort();
                String name = cacheConfig.getName();
                config.getNetworkConfig().addAddress(address);
                final HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);
                return new MapBasedBuildCacheService(
                    String.format("Hazelcast cache '%s' at %s", name, address),
                    instance.<String, byte[]>getMap(name)
                );
            }
        });
    }
}
