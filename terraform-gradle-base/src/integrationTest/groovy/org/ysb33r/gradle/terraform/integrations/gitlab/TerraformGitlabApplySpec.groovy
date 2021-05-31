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
package org.ysb33r.gradle.terraform.integrations.gitlab

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
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformGitlabApplySpec extends IntegrationSpecification {

    public static final String FILE_CONTENTS = 'foo!!'
    public static final String GITLAB_TOKEN = 'abcdefghijklo'

    File srcDir
    File destFile

    void setup() {
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        destFile = createTF()

        buildFile.text = """
        plugins {
            id 'org.ysb33r.terraform.gitlab'
        }
        
        terraformSourceSets {
            main {
                gitlab {
                    useProperty 'default', 'my.gitlab.token'       
                }
            }
        }

        tfApply {
            logProgress = true
        }
        """
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

    void 'Run terraform plan with gitlab credentials'() {
        when:
        BuildResult result = getGradleRunner(['tfPlan']).build()

        then:
        result.task(':tfPlan').outcome == SUCCESS
        result.output.contains("\"GITLAB_TOKEN\" = \"${GITLAB_TOKEN}\"")
    }

    GradleRunner getGradleRunner(List<String> tasks) {
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'tfInit',
                '-s',
                '-i',
                "-Pmy.gitlab.token=${GITLAB_TOKEN}".toString()
            ] + (tasks as List)
        )
    }

    File createTF() {
        File destFile = new File(testProjectDir.root, 'TF/foo.bar')
        new File(srcDir, 'init.tf').text = """
        terraform {
              required_providers {
                environment = {
                    source = "EppO/environment"
                    version = "1.1.0"
                }
            }
        }

        data "environment_variables" "token" {
            filter = "GITLAB_TOKEN"
        }

        resource "null_resource" "token" {
            triggers = data.environment_variables.token.items
        }
        """
        destFile
    }
}