# Gradle Hazelcast cache backend plugin

[![Build](https://github.com/sinwe/gradle-hazelcast-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/sinwe/gradle-hazelcast-plugin/actions/workflows/build.yml)
[![CodeQL](https://github.com/sinwe/gradle-hazelcast-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/sinwe/gradle-hazelcast-plugin/actions/workflows/codeql.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sinwe_gradle-hazelcast-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=sinwe_gradle-hazelcast-plugin)
[![Gradle Hazelcast Plugin](https://maven-badges.herokuapp.com/maven-central/com.github.sinwe.gradle.caching.hazelcast/gradle-hazelcast-plugin/badge.svg?style=plastic)](http://mvnrepository.com/artifact/com.github.sinwe.gradle.caching.hazelcast/gradle-hazelcast-plugin)

A simple [settings plugin](https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html) that enables [build caching](https://guides.gradle.org/using-build-cache/) in Gradle with a [Hazelcast](http://hazelcast.org) node as the backend. The Hazelcast node itself needs to be set up separately.

For a production-ready build cache implementation (featuring node management, usage statistics, health monitoring, replication, access control and more), see [Gradle Enteprise](https://gradle.com/build-cache).

For an alternative http-based build cache implementation, see [HTTP Build Cache Server](https://github.com/sinwe/http-gradle-cache-server)

## Version Compatibility Matrix

Plugin Version | Gradle Version | Hazelcast Version | Minimum JDK | Tested JDK LTS Versions | Build JDK | Notes
-------------- | -------------- | ----------------- | ----------- | ----------------------- | --------- | -----
0.17+          | 9.2+           | 5.6.0             | 17          | 17, 21, 25              | 17+       | **Gradle 9 requires Java 17+**, Hazelcast 5.6+ officially supports Java 17, tested on all LTS versions via CI
0.16           | 8.14+          | 3.10.2            | 8           | 8, 11, 17               | 11+       | Gradle 8.14, Java 8 bytecode, Hazelcast 3.10 not officially supported on Java 17
0.15           | 7.6+           | 3.10.2            | 8           | 8, 11                   | 11+       | Sonatype Central Portal support with gradle-nexus plugin 2.0.0
0.14           | 6.0+           | 3.10.2            | 8           | 8, 11                   | 8+        | Legacy OSSRH publishing
0.13 and below | 5.0+           | 3.10.2            | 8           | 8, 11                   | 8+        | Legacy versions

**Notes:**
- **Minimum JDK**: The minimum JDK version required to run Gradle (and by extension, use this plugin)
- **Tested JDK LTS Versions**: The LTS JDK versions that the plugin is known to work with. Version 0.17+ is tested in CI on all listed versions in parallel on every commit
- **Build JDK**: JDK version required to build/release the plugin itself (relevant for contributors)
- **Hazelcast Version**: Version of Hazelcast library used by the plugin. Note that you'll need to run a compatible Hazelcast server separately
- **Important for v0.17+**: Gradle 9 dropped support for Java 8-16. You must have JDK 17+ installed to run Gradle 9, even though the plugin could theoretically compile to Java 8 bytecode. Since users must have Java 17+ anyway, the plugin now targets Java 17 bytecode to take advantage of modern Java features. Additionally, Hazelcast was upgraded from 3.10.2 to 5.6.0 for official Java 17 support.

## How to use

Add this to your `settings.gradle`:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }

  dependencies {
    classpath "com.github.sinwe.gradle.caching.hazelcast:gradle-hazelcast-plugin:0.16"
  }
}

apply plugin: "com.github.sinwe.gradle.caching.hazelcast"

buildCache {
  // Note: the local cache is disabled by default when applying the plugin
  remote(com.github.sinwe.gradle.caching.hazelcast.HazelcastBuildCache) {
    host = "127.0.0.1"  //support comma separated multiple hosts
    port = 5701
    name = "gradle-build-cache"
    enabled = true
    push = true
  }
}
```

You can also specify the location and name of the Hazelcast cache via system properties (though values specified in the `settings.gradle` override the ones specified by system properties):

System property                                  | Function                        | Default value
------------------------------------------------ | ------------------------------- | ------------
`com.github.sinwe.gradle.caching.hazelcast.host` | host name of the Hazelcast node | `127.0.0.1`
`com.github.sinwe.gradle.caching.hazelcast.port` | TCP port of the Hazelcast node  | `5701`
`com.github.sinwe.gradle.caching.hazelcast.name` | name of the cache               | `gradle-task-cache`
