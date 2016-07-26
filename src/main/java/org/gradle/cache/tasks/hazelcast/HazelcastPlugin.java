package org.gradle.cache.tasks.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import groovy.lang.Closure;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.internal.tasks.cache.*;
import org.gradle.api.invocation.Gradle;

public class HazelcastPlugin implements Plugin<Gradle> {
	@Override
	public void apply(Gradle gradle) {
		gradle.getTaskCaching().useCacheFactory(new TaskOutputCacheFactory() {
			@Override
			public TaskOutputCache createCache(StartParameter startParameter) {
				ClientConfig config = new ClientConfig();
				String host = startParameter.getSystemPropertiesArgs().getOrDefault("org.gradle.cache.tasks.hazelcast.host", "127.0.0.1");
				String port = startParameter.getSystemPropertiesArgs().getOrDefault("org.gradle.cache.tasks.hazelcast.port", "5701");
				String name = startParameter.getSystemPropertiesArgs().getOrDefault("org.gradle.cache.tasks.hazelcast.name", "gradle-task-cache");
				String address = host + ":" + port;
				config.getNetworkConfig().addAddress(address);
				final HazelcastInstance instance = HazelcastClient.newHazelcastClient(config);
				gradle.buildFinished(new Closure(this) {
					public void doCall() {
						instance.shutdown();
					}
				});
				return new MapBasedTaskOutputCache(
						String.format("Hazelcast cache '%s' at %s", name, address),
						instance.getMap(name)
				) {};
			}
		});
	}
}
