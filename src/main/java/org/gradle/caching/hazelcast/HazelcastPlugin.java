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
        // Use Hazelcast as remote cache and disable local cache
        buildCacheConfiguration.getLocal().setEnabled(false);
        HazelcastBuildCache cache = buildCacheConfiguration.remote(HazelcastBuildCache.class);
        cache.setPush(true);
    }

    static class HazelcastBuildCacheServiceFactory implements BuildCacheServiceFactory<HazelcastBuildCache> {
        @Override
        public BuildCacheService createBuildCacheService(HazelcastBuildCache cacheConfig, Describer describer) {
			ClientConfig config = new ClientConfig();
			String address = cacheConfig.getHost() + ":" + cacheConfig.getPort();
			String name = cacheConfig.getName();
			config.getNetworkConfig().addAddress(address);
			HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);

			describer
                .type("Hazelcast")
			    .config("name", name)
                .config("address", address);

			return new MapBasedBuildCacheService(instance.<String, byte[]>getMap(name));
		}
    }
}
