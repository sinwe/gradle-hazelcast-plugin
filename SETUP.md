# Step-by-Step Setup for gradle-hazelcast-plugin

This guide will help you properly set up Hazelcast and integrate the `gradle-hazelcast-plugin` to ensure successful node discovery and cluster communication.

---

## Prerequisite: Setting Up Hazelcast
To ensure the `gradle-hazelcast-plugin` works correctly, you first need to set up Hazelcast and establish a functioning cluster.

### 1. Add Hazelcast to Your Project
Add the necessary Hazelcast dependencies to your project. For example, if you are using Gradle:

```gradle
dependencies {
    implementation 'com.hazelcast:hazelcast:5.2.2' // Replace with the latest version
}
```

### 2. Configure Hazelcast
Create and configure the `hazelcast.xml` or use programmatic configuration to customize your Hazelcast setup. Example programmatic configuration:

```java
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

Config config = new Config();
config.setClusterName("gradle-hazelcast-cluster");
Hazelcast.newHazelcastInstance(config);
```

### 3. Verify Node Discovery
Ensure that all nodes can discover each other in the same cluster. For simplicity, use multicast or configure static IPs for discovery:
```xml
<network>
    <join>
        <multicast enabled="true">
            <multicast-group>224.2.2.3</multicast-group>
            <multicast-port>54327</multicast-port>
        </multicast>
    </join>
</network>
```

Start multiple instances of Hazelcast to form a cluster.

---

## Step 2: Setting Up `gradle-hazelcast-plugin`

The `gradle-hazelcast-plugin` will interact with the Hazelcast cluster. Follow these steps:

### 1. Apply the Plugin in Your `build.gradle`

```gradle
plugins {
    id 'com.github.xander.plugins.gradle-hazelcast' version '1.0.0' // Replace with actual version
}
```

Ensure that the plugin version matches the desired release version. Check the [plugin's repository](https://github.com/sinwe/gradle-hazelcast-plugin) for updates.

### 2. Configure the Plugin
You need to provide the cluster configuration to the plugin. For example:

```gradle
hazelcast {
    clusterName = "gradle-hazelcast-cluster"
    clusterMembers = ["127.0.0.1"]
}
```

### 3. Verify Node Connectivity
After applying and configuring the plugin, verify that the nodes are correctly detected within the Hazelcast cluster.

1. Run a Gradle task.
2. Check the logs to ensure that the plugin successfully communicates with the nodes in the cluster.

---

## Troubleshooting

- **Node Discovery Issues**:
    - Verify that Hazelcast cluster settings (e.g., multicast group, ports) are correctly configured.
    - Ensure no firewall or network restrictions block node communication.

- **Plugin Configuration Issues**:
    - Double-check the configuration in the `build.gradle` file.
    - Refer to the [plugin's repository](https://github.com/sinwe/gradle-hazelcast-plugin) for additional setup details or open an issue if you encounter bugs.

---

With these steps completed, your Hazelcast setup and `gradle-hazelcast-plugin` integration should work seamlessly.