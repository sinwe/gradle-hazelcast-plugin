# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Gradle settings plugin that enables build caching with a Hazelcast node as the backend. The plugin implements Gradle's BuildCacheService interface using Hazelcast's distributed map as storage.

**Key characteristics:**
- Settings plugin (not a project plugin) - applied in `settings.gradle`, not `build.gradle`
- Disables local cache by default since Hazelcast serves as both local and remote cache
- Supports multiple Hazelcast hosts via comma-separated host list
- Uses Hazelcast client (not embedded node) to connect to external Hazelcast cluster

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests (includes integration tests with embedded Hazelcast)
./gradlew test

# Run single test class
./gradlew test --tests "IntegrationTest"

# Run single test method
./gradlew test --tests "IntegrationTest.no task is re-executed when inputs are unchanged"

# Check for dependency vulnerabilities
./gradlew dependencyCheckAnalyze

# Check for dependency updates
./gradlew dependenceUpdates

# Publish to local Maven repo
./gradlew publishToMavenLocal

# Release (tags, publishes to Sonatype)
./gradlew release
```

## Architecture

### Core Components

1. **HazelcastPlugin** (`src/main/java/.../HazelcastPlugin.java`)
   - Entry point: implements `Plugin<Settings>`
   - Registers `HazelcastBuildCacheServiceFactory` with Gradle's build cache configuration
   - Disables local cache and configures Hazelcast as remote cache
   - Factory creates `MapBasedBuildCacheService` backed by Hazelcast IMap

2. **HazelcastBuildCache** (`src/main/java/.../HazelcastBuildCache.java`)
   - Configuration object for the cache backend
   - Properties: `host` (supports comma-separated list), `port`, `name`
   - Reads system properties as defaults (can be overridden in settings.gradle):
     - `com.github.sinwe.gradle.caching.hazelcast.host` (default: 127.0.0.1)
     - `com.github.sinwe.gradle.caching.hazelcast.port` (default: 5701)
     - `com.github.sinwe.gradle.caching.hazelcast.name` (default: gradle-task-cache)

3. **HazelcastBuildCacheServiceFactory** (inner class in HazelcastPlugin)
   - Creates Hazelcast client with configured addresses (host:port)
   - Returns `MapBasedBuildCacheService` using Hazelcast's distributed map
   - Gradle's `MapBasedBuildCacheService` handles serialization and key-value storage

### Test Structure

- **IntegrationTest.groovy**: Spock integration tests using GradleRunner
- **HazelcastService.java**: JUnit rule that starts/stops embedded Hazelcast for tests
- Tests verify caching behavior: cache hits, cache misses, task outcomes
- Uses custom port (5710) to avoid conflicts with running Hazelcast instances

## Project Configuration

- **Java 8 compatibility** (sourceCompatibility/targetCompatibility = 1.8)
- **Gradle 7.6.4** with Java 8+ runtime
- **Dependencies managed in versions.gradle**
- **Hazelcast 3.10.2** (client and full distribution for tests)

## Publishing

The plugin publishes to:
1. **Gradle Plugin Portal** (via `repo.gradle.org/gradle` with Artifactory)
2. **Maven Central** (via Central Portal at `central.sonatype.com`)

Release process uses `net.researchgate.release` plugin:
- Creates git tag
- Uses `publish` task to upload to Maven Central Portal
- Requires signing for non-SNAPSHOT versions

### Credentials

For publishing to Maven Central Portal, set these properties in `~/.gradle/gradle.properties`:
```properties
mavenCentralUsername=<your-token-username>
mavenCentralPassword=<your-token-password>
```

For Artifactory (Gradle internal repo):
```properties
artifactory_user=<your-username>
artifactory_password=<your-password>
```

Note: OSSRH (oss.sonatype.org) was shut down on June 30, 2025. All publishing now goes through Central Portal.

## Important Implementation Details

- Plugin must be applied in `settings.gradle` (not `build.gradle`) because it configures build cache during settings evaluation
- Hazelcast client connection is created once per build, map name identifies the cache
- Multiple hosts support allows failover: "host1,host2,host3" all use same port
- MapBasedBuildCacheService is Gradle's built-in implementation - we just provide the Map
- Local cache disabled to avoid double-caching (Hazelcast already provides fast access)
