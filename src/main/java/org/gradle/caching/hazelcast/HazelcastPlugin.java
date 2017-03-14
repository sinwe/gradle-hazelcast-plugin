package org.gradle.caching.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.BuildCacheService;
import org.gradle.caching.BuildCacheServiceFactory;
import org.gradle.caching.MapBasedBuildCacheService;
import org.gradle.caching.configuration.BuildCacheConfiguration;

/**
 * {@link Settings} plugin to register Hazelcast as a build cache backend.
 *
 * @see HazelcastBuildCache
 */
public class HazelcastPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings settings) {
        BuildCacheConfiguration buildCacheConfiguration = settings.getBuildCache();
        buildCacheConfiguration.registerBuildCacheService(HazelcastBuildCache.class, HazelcastBuildCacheServiceFactory.class);
        // Use Hazelcast as local cache
        buildCacheConfiguration.local(HazelcastBuildCache.class);
    }

    static class HazelcastBuildCacheServiceFactory implements BuildCacheServiceFactory<HazelcastBuildCache> {
        @Override
        public BuildCacheService createBuildCacheService(HazelcastBuildCache cacheConfig) {
			ClientConfig config = new ClientConfig();
			String address = cacheConfig.getHost() + ":" + cacheConfig.getPort();
			String name = cacheConfig.getName();
			config.getNetworkConfig().addAddress(address);
			final HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);
			return new MapBasedBuildCacheService(
				String.format("Hazelcast node '%s' at %s", name, address),
				instance.<String, byte[]>getMap(name)
			);
		}
    }
}
