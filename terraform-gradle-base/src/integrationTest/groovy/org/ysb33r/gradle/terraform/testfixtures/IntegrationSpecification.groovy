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
package org.ysb33r.gradle.terraform.testfixtures

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ysb33r.grolifant.api.core.OperatingSystem

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.FileVisitResult.CONTINUE

class IntegrationSpecification extends DownloadTestSpecification {
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
        List<String> eventualTaskNames = ['-s']
        eventualTaskNames.addAll(taskNames)

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
        projectDir = testProjectDir.root
        buildDir = new File(projectDir, 'build')
        projectCacheDir = new File(projectDir, '.gradle')
        buildFile = new File(projectDir, 'build.gradle')
    }

    void cleanup() {
        Files.walkFileTree(
            new File(projectDir, 'build/tf/main/plugins').toPath(),
            new FileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    CONTINUE
                }

                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.symbolicLink) {
                        println "Deleting: ${file}"
                        Files.delete(file)
                    }
                    CONTINUE
                }

                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    println "Failed to visit: ${file}, because ${exc.message}"
                    CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    CONTINUE
                }
            }
        )
    }
}
