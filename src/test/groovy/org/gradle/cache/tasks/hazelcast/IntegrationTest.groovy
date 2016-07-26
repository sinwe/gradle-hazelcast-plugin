package org.gradle.cache.tasks.hazelcast

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class IntegrationTest extends Specification {
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
    List<String> skippedTasks
    List<String> nonSkippedTasks

    @Rule HazelcastService hazelcastService = new HazelcastService();

    def setup() {
        buildFile = testProjectDir.newFile("build.gradle")

        testProjectDir.newFile("init.gradle") << """
            initscript {
                dependencies {
                    classpath files($CLASSPATH)
                }
            }
            apply plugin: org.gradle.cache.tasks.hazelcast.HazelcastPlugin
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
        skippedTasks.empty

        expect:
        succeeds "clean"

        when:
        succeeds "jar"
        then:
        skippedTasks.containsAll ":compileJava", ":jar"
    }

    BuildResult succeeds(String... tasks) {
        arguments.addAll "-Dorg.gradle.cache.tasks=true", "--init-script", "init.gradle"
        arguments.addAll tasks
        def result = GradleRunner.create()
            .forwardOutput()
            .withProjectDir(testProjectDir.root)
            .withArguments(arguments)
            .withPluginClasspath()
            .build()
        assert result.taskPaths(FAILED).empty
        skippedTasks = result.taskPaths(UP_TO_DATE) + result.taskPaths(SKIPPED)
        nonSkippedTasks = result.taskPaths(SUCCESS)
        return result
    }
}
