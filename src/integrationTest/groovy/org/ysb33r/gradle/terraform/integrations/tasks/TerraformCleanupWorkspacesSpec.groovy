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
package org.ysb33r.gradle.terraform.integrations.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import org.ysb33r.gradle.terraform.integrations.IntegrationSpecification
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.FileVisitResult.CONTINUE
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformCleanupWorkspacesSpec extends IntegrationSpecification {

    public static final String FILE_CONTENTS = 'foo!!'
    public static final List<String> TEST_WORKSPACES = ['alpha', 'beta']
    public static final String WITH_WORKSPACES = """workspaces '${TEST_WORKSPACES.join("','")}'"""
    public static final String WITHOUT_WORKSPACES = ''

    File srcDir
    File destFile

    void setup() {
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        destFile = createTF()
        writeBuildFile(WITH_WORKSPACES)
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

    void 'Run terraform apply on different workspaces'() {
//        setup:
//        File planFile = new File(buildDir, 'tf/main/main.tf.plan')
//        File textFile = new File(buildDir, "reports/tf/main/main.tf.plan.${json ? 'json' : 'txt'}")

        when: 'running with default workspace'
        BuildResult defaultResult = getGradleRunner(['tfApply']).build()

        then:
        defaultResult.task(':tfInit').outcome == SUCCESS
        defaultResult.task(':tfApply').outcome == SUCCESS

        when: 'running with first workspace'
        BuildResult alphaResult = getGradleRunner(['tfApplyAlpha']).build()

        then:
        alphaResult.task(':tfApplyAlpha').outcome == SUCCESS

        when: 'running with second workspace'
        BuildResult betaResult = getGradleRunner(['tfApplyBeta']).build()

        then:
        betaResult.task(':tfApplyBeta').outcome == SUCCESS

        and: 'tfPlan should not have executed'
        betaResult.task(':tfPlan') == null

        and: 'tfInitBeta should not exist as a task'
        betaResult.task(':tfInitBeta') == null

        when: 'removing the workspaces'
        writeBuildFile(WITHOUT_WORKSPACES)
        BuildResult noWorkspaceResult = getGradleRunner(['tfApply']).build()

        then:
        noWorkspaceResult.task(':tfInit').outcome == SUCCESS

        when: 'running a task for a workspace that should no longer exists'
        BuildResult noWorkspaceResult2 = getGradleRunner(['tfApplyAlpha']).buildAndFail()

        then:
        noWorkspaceResult2.output.contains("Task 'tfApplyAlpha' not found")

        when: 'cleaning up old workspaces without approval'
        BuildResult cleaningResult = getGradleRunner(['tfCleanupWorkspaces']).buildAndFail()

        then: 'fails due to dangling workspaces'
        cleaningResult.task(':tfCleanupWorkspaces').outcome == FAILED
        cleaningResult.output.contains('Workspace "alpha" is not empty.')

        when: 'cleaning up old workspaces with force'
        BuildResult cleaningResult2 = getGradleRunner(['tfCleanupWorkspaces', '--force']).build()

        then: 'removes local workspaces'
        cleaningResult2.task(':tfCleanupWorkspaces').outcome == SUCCESS
    }

    void 'Run apply for multiple workspaces in one command-line'() {
        when:
        BuildResult result = getGradleRunner(['tfApply', 'tfApplyAlpha', 'tfApplyBeta']).build()

        then:
        result.task(':tfApply').outcome == SUCCESS
        result.task(':tfApplyAlpha').outcome == SUCCESS
        result.task(':tfApplyBeta').outcome == SUCCESS
    }

    GradleRunner getGradleRunner(List<String> tasks) {
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'tfInit',
                '-s',
                '-i'
            ] + (tasks as List)
        )
    }

    void writeBuildFile(String workspaces) {
        String path = destFile.canonicalPath
        buildFile.text = """
        plugins {
            id 'org.ysb33r.terraform'
        }
        
        terraformSourceSets {
            main {
                variables {
                    var 'foofile', '${OS.windows ? path.replaceAll(~/\x5C/, '/') : path}'
                }
                
                ${workspaces}
            }
        }

        tfApply {
            logProgress = true
        }
        """
    }

    File createTF() {
        File destFile = new File(testProjectDir.root, 'TF/foo.bar')
        new File(srcDir, 'init.tf').text = """
        terraform {
              required_providers {
                local = {
                    source = "hashicorp/local"
                    version = "2.1.0"
                }
            }
        }

        variable "foofile" {
          type = string
        }
        
        resource "local_file" "foo" {
            content     = "${FILE_CONTENTS}"
            filename = var.foofile
        }
        """
        destFile
    }
}