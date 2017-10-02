package org.ysb33r.gradle.terraform.integrations

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.Specification


class IntegrationSpecification extends Specification {
    public static final OperatingSystem OS = OperatingSystem.current()
    public static final boolean IS_KOTLIN_DSL = false
    public static final boolean IS_GROOVY_DSL = true

    static GradleRunner getGradleRunner(
        boolean groovyDsl,
        File projectDir,
        String taskName
    ) {
        getGradleRunner(groovyDsl, projectDir, [taskName])
    }

    static GradleRunner getGradleRunner(
        boolean groovyDsl,
        File projectDir,
        List<String> taskNames
    ) {
        List<String> eventualTaskNames = []
        eventualTaskNames.addAll(taskNames)

        if (OS.windows) {
            eventualTaskNames.add '--no-daemon'
        }

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(eventualTaskNames)
            .forwardOutput()

        runner.withDebug(groovyDsl)
        runner.withPluginClasspath()
    }

    @Rule
    TemporaryFolder testProjectDir

    File projectDir
    File buildDir
    File projectCacheDir
    File buildFile

    void setup() {
        projectDir = testProjectDir.root
        buildDir = new File(projectDir, 'build')
        projectCacheDir = new File(projectDir, '.gradle')
        buildFile = new File(projectDir, 'build.gradle')
    }
}
