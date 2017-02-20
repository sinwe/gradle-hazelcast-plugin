package org.gradle.caching.hazelcast

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class IntegrationTest extends Specification {
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

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    final arguments = []
    File buildFile
    List<String> cachedTasks
    List<String> executedTasks

    @Rule HazelcastService hazelcastService = new HazelcastService(HAZELCAST_PORT)

    def setup() {
        buildFile = testProjectDir.newFile("build.gradle")

        testProjectDir.newFile("settings.gradle") << """
            rootProject.name = 'test'
            gradle.startParameter.taskOutputCacheEnabled = true

            buildscript {
                dependencies {
                    classpath files($CLASSPATH)
                }
            }
            apply plugin: $HazelcastPlugin.name

            buildCache {
                // Disable local cache, as Hazelcast will serve as both local and remote
                local {
                  enabled = false
                }
                remote($HazelcastBuildCache.name) {
                    port = $HAZELCAST_PORT
                }
            }
        """

        buildFile << """
            apply plugin: "java"
        """

        testProjectDir.newFolder "src", "main", "java"
        testProjectDir.newFile("src/main/java/Hello.java") << ORIGINAL_HELLO_WORLD
        testProjectDir.newFolder "src", "main", "resources"
        testProjectDir.newFile("src/main/resources/resource.properties") << """
            test=true
        """
    }

    def "no task is re-executed when inputs are unchanged"() {
        when:
        succeeds "jar"
        then:
        cachedTasks.empty

        expect:
        succeeds "clean"

        when:
        succeeds "jar"
        then:
        cachedTasks.containsAll ":compileJava", ":jar"
    }

    def "outputs are correctly loaded from cache"() {
        buildFile << """
            apply plugin: "application"
            mainClassName = "Hello"
        """
        succeeds "run"
        succeeds "clean"
        expect:
        succeeds "run"
    }

    def "tasks get cached when source code changes without changing the compiled output"() {
        when:
        succeeds "assemble"
        then:
        cachedTasks.empty

        succeeds "clean"

        when:
        file("src/main/java/Hello.java") << """
            // Change to source file without compiled result change
        """
        succeeds "assemble"
        then:
        executedTasks.contains ":compileJava"
        cachedTasks.contains ":jar"
    }

    def "tasks get cached when source code changes back to previous state"() {
        expect:
        succeeds "jar"
        executedTasks.containsAll ":compileJava", ":jar"

        when:
        file("src/main/java/Hello.java").text = CHANGED_HELLO_WORLD
        then:
        succeeds "jar"
        executedTasks.containsAll ":compileJava", ":jar"

        println "\n\n\n-----------------------------------------\n\n\n"

        when:
        file("src/main/java/Hello.java").text = ORIGINAL_HELLO_WORLD
        then:
        succeeds "jar"
        cachedTasks.containsAll ":compileJava", ":jar"
    }

    def "jar tasks get cached even when output file is changed"() {
        when:
        succeeds "assemble"
        then:
        cachedTasks.empty
        file("build/libs/test.jar").isFile()

        when:
        file("build").deleteDir()
        then:
        !file("build/libs/test.jar").isFile()

        when:
        buildFile << """
            jar {
                destinationDir = file("build/other-jar")
                baseName = "other-jar"
            }
        """

        succeeds "assemble"
        then:
        cachedTasks.contains ":jar"
        !file("build/libs/test.jar").isFile()
        file("build/other-jar/other-jar.jar").isFile()
    }

    def "clean doesn't get cached"() {
        succeeds "assemble"
        succeeds "clean"
        succeeds "assemble"
        when:
        succeeds "clean"
        then:
        executedTasks.contains ":clean"
    }

    def "cacheable task with cache disabled doesn't get cached"() {
        buildFile << """
            compileJava.outputs.cacheIf { false }
        """

        succeeds "compileJava"
        succeeds "clean"

        when:
        succeeds "compileJava"
        then:
        // :compileJava is not cached, but :jar is still cached as its inputs haven't changed
        executedTasks.contains ":compileJava"
    }

    def "non-cacheable task with cache enabled gets cached"() {
        testProjectDir.newFile("input.txt") << "data"
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
        succeeds "jar"
        then:
        executedTasks.contains ":customTask"

        when:
        succeeds "clean"
        succeeds "jar"
        then:
        cachedTasks.contains ":customTask"
    }

    BuildResult succeeds(String... tasks) {
        arguments.add "--stacktrace"
        arguments.addAll tasks
        def result = GradleRunner.create()
            .forwardOutput()
            .withProjectDir(testProjectDir.root)
            .withArguments(arguments)
            .build()
        assert result.taskPaths(FAILED).empty
        cachedTasks = result.taskPaths(FROM_CACHE)
        executedTasks = result.taskPaths(SUCCESS)
        return result
    }

    File file(String path) {
        return new File(testProjectDir.root, path)
    }
}
