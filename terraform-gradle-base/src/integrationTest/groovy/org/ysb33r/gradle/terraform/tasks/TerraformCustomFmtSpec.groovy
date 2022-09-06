/*
 * Copyright 2017-2022 the original author or authors.
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
package org.ysb33r.gradle.terraform.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.testfixtures.DownloadTestSpecification
import org.ysb33r.gradle.terraform.testfixtures.IntegrationSpecification
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.gradle.terraform.plugins.TerraformPlugin.CUSTOM_FMT_APPLY
import static org.ysb33r.gradle.terraform.plugins.TerraformPlugin.CUSTOM_FMT_CHECK

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformCustomFmtSpec extends IntegrationSpecification {

    File testkitDir
    File srcDir

    void setup() {
        testkitDir = testProjectDir.newFolder()
        srcDir = new File(projectDir, 'customDir')
        srcDir.mkdirs()
        createTF()

        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform'
        }
        '''
    }

    void 'Run tfFmt*CustomDirectories without directories will skip task'() {
        when:
        BuildResult result1 = getGradleRunner(CUSTOM_FMT_CHECK).build()
        BuildResult result2 = getGradleRunner(CUSTOM_FMT_APPLY).build()

        then:
        result1.task(":${CUSTOM_FMT_CHECK}").outcome == NO_SOURCE
        result2.task(":${CUSTOM_FMT_APPLY}").outcome == NO_SOURCE
    }

    void 'tfFmtCheckCustomDirectories fails build if source is incorrectly formatted'() {
        setup:
        def taskName = CUSTOM_FMT_CHECK
        buildFile << "${CUSTOM_FMT_CHECK}.dirs 'customDir'"

        when:
        BuildResult result = getGradleRunner(taskName).buildAndFail()

        then:
        result.task(":${taskName}").outcome == FAILED
        result.output.contains('Source format does not match convention')
    }

    void 'Correctly formatted code will not fail build'() {
        setup:
        buildFile << "${CUSTOM_FMT_CHECK}.dirs 'customDir'"

        when:
        BuildResult result = getGradleRunner(CUSTOM_FMT_APPLY).build()

        then:
        result.task(":${CUSTOM_FMT_APPLY}").outcome == SUCCESS
        result.task(":${CUSTOM_FMT_CHECK}") == null

        when:
        result = getGradleRunner(CUSTOM_FMT_CHECK).build()

        then:
        result.task(":${CUSTOM_FMT_CHECK}").outcome == SUCCESS
    }

    GradleRunner getGradleRunner(String taskName) {
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                taskName,
                '-s', '-i'
            ]
        ).withTestKitDir(testkitDir)
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
            content     = "abcdef"
            filename = var.foofile
        }
        """
        destFile
    }

}