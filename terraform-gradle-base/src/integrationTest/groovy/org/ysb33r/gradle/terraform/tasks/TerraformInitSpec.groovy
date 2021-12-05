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
package org.ysb33r.gradle.terraform.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.testfixtures.DownloadTestSpecification
import org.ysb33r.gradle.terraform.testfixtures.IntegrationSpecification
import org.ysb33r.grashicorp.HashicorpUtils
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformInitSpec extends IntegrationSpecification {

    String taskName = 'tfInit'
    File testkitDir
    File srcDir
    GradleRunner gradleRunner

    void setup() {
        testkitDir = testProjectDir.newFolder()
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()

        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform'
        }
        '''

        gradleRunner = getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                taskName,
                '-s'
            ]
        ).withTestKitDir(testkitDir)
    }

    void 'Run terraform init on a clean project directory'() {
        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == NO_SOURCE
    }

    @SuppressWarnings('LineLength')
    void 'Run terraform (0.13+) init on a project with a single plugin'() {
        setup:
        String providerVersion = '2.70.0'
        File pluginDir = new File(
            testkitDir,
            "caches/terraform.d/registry.terraform.io/hashicorp/aws/${providerVersion}/${HashicorpUtils.osArch(DownloadTestSpecification.OS)}"
        )
        new File(srcDir, 'init.tf').text = """
        provider "aws" {
          version = "${providerVersion}"
          region  = "us-east-1"
        }
        """

        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS
        pluginDir.exists()
        pluginDir.listFiles().find { it.name.startsWith('terraform-') }
        new File(testkitDir, 'caches/terraform.d').exists()
    }

    void 'Run terraform (0.12) init on a project with a single plugin'() {
        setup:
        buildFile << '''
        terraform {
            executable version : '0.12.24'
        }
        '''
        File pluginDir = new File(
            testkitDir,
            "caches/terraform.d/${HashicorpUtils.osArch(DownloadTestSpecification.OS)}"
        )
        new File(srcDir, 'init.tf').text = '''
provider "aws" {
  version = "~> 2.0"
  region  = "us-east-1"
}
        '''

        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS
        pluginDir.exists()
        pluginDir.listFiles().find { it.name.startsWith('terraform-') }
    }

    void '--reconfigure will cause out of date condition'() {
        setup:
        new File(srcDir, 'init.tf').text = """
        terraform {
          required_providers {
            aws = {
              source = "hashicorp/aws"
            }
          }
          required_version = ">= 0.13"
        }

        provider "aws" {
          region  = "us-east-1"
        }
        """

        when: 'tfInit is run the first time'
        BuildResult result1 = gradleRunner.build()

        then: 'the task is executed'
        result1.task(":${taskName}").outcome == SUCCESS

        when: 'tfInit is run the second time'
        BuildResult result2 = gradleRunner.build()

        then: 'the task is not executed'
        result2.task(":${taskName}").outcome == UP_TO_DATE

        when: 'tfInit is run with --reconfigure'
        BuildResult result3 = nextRunner('--reconfigure').build()

        then: 'the task is executed'
        result3.task(":${taskName}").outcome == SUCCESS

        when: 'tfInit is run with --force-copy'
        BuildResult result4 = nextRunner('--force-copy').build()

        then: 'the task is executed'
        result4.task(":${taskName}").outcome == SUCCESS

        when: 'tfInit is run with --upgrade'
        BuildResult result5 = nextRunner('--upgrade').build()

        then: 'the task is executed'
        result5.task(":${taskName}").outcome == SUCCESS

        when: 'tfInit is then run again without parameters'
        BuildResult result6 = nextRunner().build()

        then: 'the task is not executed'
        result6.task(":${taskName}").outcome == UP_TO_DATE
    }

    GradleRunner nextRunner(String... args) {
        List<String> args1 = [
            taskName,
            '-s', '-i'
        ]
        args1.addAll(args)
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            args1
        ).withTestKitDir(testkitDir)
    }

}