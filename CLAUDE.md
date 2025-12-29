# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Gradle settings plugin that enables build caching using Hazelcast as the cache backend. The plugin allows Gradle builds to store and retrieve task outputs from a distributed Hazelcast cluster, speeding up builds across multiple machines or build agents.

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Check for dependency vulnerabilities (OWASP)
./gradlew dependencyCheckAnalyze

# Check for dependency updates
./gradlew dependenceiesUpdates
```

## Publishing Commands

```bash
# Publish to local Maven repository for testing
./gradlew publishToMavenLocal

# Release workflow (requires credentials and JDK 11+)
./gradlew release  # This creates a git tag, publishes, and closes the staging repository

# Publish manually to Central Portal (for testing)
./gradlew publishToSonatype

# Close staging repository (after publishToSonatype)
./gradlew closeSonatypeStagingRepository

# Release to Maven Central (manually after closing - requires separate invocation)
./gradlew findSonatypeStagingRepository releaseSonatypeStagingRepository

# All-in-one manual publish and close
./gradlew publishToSonatype closeSonatypeStagingRepository
```

The release process is automated using the `net.researchgate.release` plugin. After release, the `afterReleaseBuild` task automatically publishes and closes the staging repository using gradle-nexus/publish-plugin 2.0.0. You must then manually release via the Portal UI at [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) or run `releaseSonatypeStagingRepository` separately.

## Architecture

### Core Components

**HazelcastPlugin** (`src/main/java/com/github/sinwe/gradle/caching/hazelcast/HazelcastPlugin.java`)
- Entry point - implements Gradle's `Plugin<Settings>` interface
- Registers `HazelcastBuildCache` as a build cache service via `HazelcastBuildCacheServiceFactory`
- Automatically disables local cache and configures Hazelcast as the remote cache
- The factory creates a Hazelcast client connection and wraps the Hazelcast IMap in Gradle's `MapBasedBuildCacheService`

**HazelcastBuildCache** (`src/main/java/com/github/sinwe/gradle/caching/hazelcast/HazelcastBuildCache.java`)
- Configuration class extending `AbstractBuildCache`
- Defines three configurable properties: `host`, `port`, and `name`
- Supports system property overrides: `com.github.sinwe.gradle.caching.hazelcast.{host,port,name}`
- Default values: host=`127.0.0.1`, port=`5701`, name=`gradle-task-cache`
- The `host` property supports comma-separated values for multiple Hazelcast nodes

### Key Design Decisions

1. **Settings Plugin**: This is a settings plugin (not a project plugin), so it's applied in `settings.gradle` rather than `build.gradle`. This is required because build cache configuration happens at settings evaluation time.

2. **MapBasedBuildCacheService**: The plugin leverages Gradle's built-in `MapBasedBuildCacheService` which wraps any `java.util.Map` implementation. The Hazelcast `IMap` is a distributed map that naturally fits this pattern.

3. **Client Connection**: Uses Hazelcast client mode (not embedded), connecting to an external Hazelcast cluster. The connection is established once per build when the factory creates the service.

4. **Multiple Hosts**: The factory parses the comma-separated host list and appends the port to each host address before configuring the Hazelcast client's network config.

## Testing

**IntegrationTest.groovy** (`src/test/groovy/com/github/sinwe/gradle/caching/hazelcast/IntegrationTest.groovy`)
- Uses Gradle TestKit to run actual Gradle builds
- Tests verify tasks are cached and retrieved correctly
- The `HazelcastService` JUnit rule starts an embedded Hazelcast instance on port 5710 for testing

**HazelcastService.java** (`src/test/groovy/com/github/sinwe/gradle/caching/hazelcast/HazelcastService.java`)
- JUnit `ExternalResource` that manages a test Hazelcast instance lifecycle
- Configures Hazelcast with multicast disabled (using the specified port only)
- Automatically starts before each test and shuts down after

## Dependencies Management

Dependencies are centralized in `versions.gradle` using `ext.libraries` map. When updating dependencies:
- Update version numbers in the `ext` block at the top of `versions.gradle`
- Reference them via the `libraries` map in `build.gradle`

Key dependencies:
- `hazelcast-client`: Used by the plugin to connect to Hazelcast
- `hazelcast`: Used only in tests for the embedded instance
- Gradle plugin dependencies: release plugin, OWASP dependency check, version checker

## Gradle Configuration

The project uses:
- Gradle 9.2.1 (via Gradle wrapper)
- Java 17 source/target compatibility (Gradle 9 requires JDK 17+ to run)
- `java-gradle-plugin` for automatic plugin metadata generation
- Plugin ID: `com.github.sinwe.gradle.caching.hazelcast`
- Group: `com.github.sinwe.gradle.caching.hazelcast`
- Artifact: `gradle-hazelcast-plugin`

**Note**: Gradle 9 dropped support for Java 8-16. Since users must have JDK 17+ to run Gradle 9, the plugin now targets Java 17 bytecode to leverage modern language features.

## Publishing Configuration

The project uses the `io.github.gradle-nexus.publish-plugin` (version 2.0.0) for publishing to Sonatype Central Portal.

Publications go to:
- **Releases/Staging**: `https://ossrh-staging-api.central.sonatype.com/service/local/`
- **Snapshots**: `https://central.sonatype.com/repository/maven-snapshots/`

### Authentication Setup

Credentials are sourced from (in order of precedence):
1. Gradle properties in `~/.gradle/gradle.properties`:
   - `sonatypeUsername` - User token username from central.sonatype.com/account
   - `sonatypePassword` - User token password from central.sonatype.com/account
2. Environment variables:
   - `ORG_GRADLE_PROJECT_sonatypeUsername`
   - `ORG_GRADLE_PROJECT_sonatypePassword`

**Important**: Central Portal tokens are different from legacy OSSRH tokens. Generate new tokens at https://central.sonatype.com/account

### Publication Artifacts

Artifacts include:
- Main JAR (from Java component)
- Sources JAR
- Javadoc JAR

Signing is required only for release versions (non-SNAPSHOT).

### Migration Notes

This project has been migrated from the legacy OSSRH system to Sonatype Central Portal (as of 2025). The OSSRH service was sunset on June 30, 2025. Key changes:
- New authentication tokens required from central.sonatype.com
- Publishing workflow now uses staging repositories with explicit close/release steps
- Publication timeline: ~15 minutes after release for artifacts to appear on Maven Central
