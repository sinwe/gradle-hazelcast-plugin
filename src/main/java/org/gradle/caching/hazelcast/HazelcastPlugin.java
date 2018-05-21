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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            int port = cacheConfig.getPort();
            List<String> addressList = Stream.of(cacheConfig.getHost()
                .split(","))
                .map(String::trim)
                .map(address -> address + ":" + port)
                .collect(Collectors.toList());
            String name = cacheConfig.getName();
			config.getNetworkConfig().addAddress(addressList.toArray(new String[0]));
			HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);

			describer
                .type("Hazelcast")
			    .config("name", name)
                .config("address", addressList.toString());

			return new MapBasedBuildCacheService(instance.getMap(name));
		}
    }
}
