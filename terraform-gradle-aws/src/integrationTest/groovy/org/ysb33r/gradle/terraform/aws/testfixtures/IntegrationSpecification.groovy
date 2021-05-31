/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform.aws.testfixtures

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ysb33r.grolifant.api.core.OperatingSystem
import spock.lang.Specification

class IntegrationSpecification extends Specification {
    public static final File TERRAFORM_CACHE_DIR = new File(
        System.getProperty('TERRAFORM_CACHE_DIR') ?: '../build/terraform-binaries',
        'terraform'
    ).absoluteFile
//    public static final File RESOURCES_DIR = new File(System.getProperty('RESOURCES_DIR') ?:
//        './src/downloadTest/resources')

    public static final OperatingSystem OS = OperatingSystem.current()
    public static final boolean SKIP_TESTS = !(OS.macOsX || OS.linux || OS.windows || OS.freeBSD)
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
        eventualTaskNames.add("-Dorg.ysb33r.gradle.terraform.uri=${TERRAFORM_CACHE_DIR.toURI()}".toString())

        GradleRunner runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(eventualTaskNames)
            .forwardOutput()
            .withTestKitDir(new File(projectDir, '.testkit'))

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
//        System.setProperty('org.ysb33r.gradle.terraform.uri', TERRAFORM_CACHE_DIR.toURI().toString())
        projectDir = testProjectDir.root
        buildDir = new File(projectDir, 'build')
        projectCacheDir = new File(projectDir, '.gradle')
        buildFile = new File(projectDir, 'build.gradle')
    }
}
