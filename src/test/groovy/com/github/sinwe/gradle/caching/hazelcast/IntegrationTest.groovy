package com.github.sinwe.gradle.caching.hazelcast

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class IntegrationTest extends Specification {
    // Gradle versions to test against (only GA releases)
    // Note: Use full version numbers (e.g., "9.0.0", not "9.0") as Gradle downloads require exact versions
    //
    // Gradle 8.x: Excluded on JDK 25+ — Gradle 8.x bundles ASM 9.7.1 which only supports class files up to JDK 24.
    // Gradle 9.x: All GA releases (current plugin targets 9.2+); Gradle 9.1+ supports JDK 25.
    static final List<String> GRADLE_VERSIONS = ([
        // Gradle 8.x series (last stable release before 9.0)
        "8.14.2",

        // Gradle 9.x series (all GA releases)
        "9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1", "9.5.1"
    ] as List<String>).findAll { String version ->
        version.startsWith("9.") || Runtime.version().feature() < 25
    }
    public static final int HAZELCAST_PORT = 5710
    public static final String ORIGINAL_HELLO_WORLD = """
            public class Hello {
                public static void main(String... args) {
                    System.out.println("Hello World!");
                }
            }
        """
    public static final String CHANGED_HELLO_WORLD = """
            public class Hello {
                public static void main(String... args) {
                    System.out.println("Hello World with Changes!");
                }
            }
        """
    public static final String CLASSPATH = IntegrationTest.class.classLoader.getResource("plugin-classpath.txt").text.split("\n").collect { path -> "'$path'" }.join(", ")

    @TempDir File testProjectDir
    final arguments = []
    File buildFile
    List<String> cachedTasks
    List<String> executedTasks
    HazelcastService hazelcastService
    String currentGradleVersion
    String cacheName = UUID.randomUUID().toString()

    def setup() {
        hazelcastService = new HazelcastService(HAZELCAST_PORT)
        hazelcastService.start()

        buildFile = new File(testProjectDir, "build.gradle")
        buildFile.createNewFile()

        new File(testProjectDir, "settings.gradle").text = """
            rootProject.name = 'test'

            buildscript {
                dependencies {
                    classpath files($CLASSPATH)
                }
            }
            apply plugin: $HazelcastPlugin.name

            buildCache {
                remote($HazelcastBuildCache.name) {
                    port = $HAZELCAST_PORT
                    push = true
                    name = '$cacheName'
                }
            }
        """

        buildFile << """
            apply plugin: "java"
        """

        new File(testProjectDir, "src/main/java").mkdirs()
        new File(testProjectDir, "src/main/java/Hello.java").text = ORIGINAL_HELLO_WORLD
        new File(testProjectDir, "src/main/resources").mkdirs()
        new File(testProjectDir, "src/main/resources/resource.properties").text = """
            test=true
        """
    }

    def cleanup() {
        hazelcastService?.stop()
    }

    @Unroll
    def "no task is re-executed when inputs are unchanged - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion

        when:
        succeeds "compileJava"
        then:
        cachedTasks.empty

        expect:
        succeeds "clean"

        when:
        succeeds "compileJava"
        then:
        cachedTasks.containsAll ":compileJava"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    @Unroll
    def "outputs are correctly loaded from cache - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion
        buildFile << """
            apply plugin: "application"
            application {
                mainClass = "Hello"
            }
        """

        when:
        succeeds "run"
        succeeds "clean"

        then:
        succeeds "run"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    @Unroll
    def "tasks get cached when source code changes back to previous state - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion

        expect:
        succeeds "compileJava"
        executedTasks.containsAll ":compileJava"

        when:
        file("src/main/java/Hello.java").text = CHANGED_HELLO_WORLD
        then:
        succeeds "compileJava"
        executedTasks.containsAll ":compileJava"

        println "\n\n\n-----------------------------------------\n\n\n"

        when:
        file("src/main/java/Hello.java").text = ORIGINAL_HELLO_WORLD
        then:
        succeeds "compileJava"
        cachedTasks.containsAll ":compileJava"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    @Unroll
    def "clean doesn't get cached - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion

        when:
        succeeds "assemble"
        succeeds "clean"
        succeeds "assemble"
        succeeds "clean"

        then:
        executedTasks.contains ":clean"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    @Unroll
    def "cacheable task with cache disabled doesn't get cached - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion
        buildFile << """
            compileJava.outputs.cacheIf { false }
        """

        when:
        succeeds "compileJava"
        succeeds "clean"
        succeeds "compileJava"

        then:
        // :compileJava is not cached, but :jar is still cached as its inputs haven't changed
        executedTasks.contains ":compileJava"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    @Unroll
    def "non-cacheable task with cache enabled gets cached - Gradle #gradleVersion"() {
        given:
        currentGradleVersion = gradleVersion
        new File(testProjectDir, "input.txt").text = "data"
        buildFile << """
            class NonCacheableTask extends DefaultTask {
                @InputFile inputFile
                @OutputFile outputFile

                @TaskAction copy() {
                    project.mkdir outputFile.parentFile
                    outputFile.text = inputFile.text
                }
            }
            task customTask(type: NonCacheableTask) {
                inputFile = file("input.txt")
                outputFile = file("\$buildDir/output.txt")
                outputs.cacheIf { true }
            }
            compileJava.dependsOn customTask
        """

        when:
        succeeds "compileJava"
        then:
        executedTasks.contains ":customTask"

        when:
        succeeds "clean"
        succeeds "compileJava"
        then:
        cachedTasks.contains ":customTask"

        where:
        gradleVersion << GRADLE_VERSIONS
    }

    BuildResult succeeds(String... tasks) {
        arguments.add "--build-cache"
        arguments.add "--stacktrace"
        arguments.addAll tasks
        def runner = GradleRunner.create()
            .forwardOutput()
            .withProjectDir(testProjectDir)
            .withArguments(arguments)

        // Use specific Gradle version if set, otherwise use wrapper version
        if (currentGradleVersion) {
            runner.withGradleVersion(currentGradleVersion)
        }

        def result = runner.build()
        assert result.taskPaths(FAILED).empty
        cachedTasks = result.taskPaths(FROM_CACHE)
        executedTasks = result.taskPaths(SUCCESS)
        return result
    }

    File file(String path) {
        return new File(testProjectDir, path)
    }
}
