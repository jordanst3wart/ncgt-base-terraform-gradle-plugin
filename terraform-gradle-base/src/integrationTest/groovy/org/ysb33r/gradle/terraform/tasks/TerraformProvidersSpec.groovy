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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformProvidersSpec extends IntegrationSpecification {

    String taskName = 'tfInit'
    File testkitDir
    File srcDir
    File reportsDir

    void setup() {
        testkitDir = testProjectDir.newFolder()
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()

        reportsDir = new File(projectDir,'build/reports/tf/main')

        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform'
        }
        
        terraform {
          platforms allPlatforms
        }
        '''

        createTfSource()
    }

    void 'tfProvidersShow describes the providers on the console'() {
        when:
        BuildResult result1 = nextRunner('tfProvidersShow').build()

        then:
        result1.task(":tfProvidersShow").outcome == SUCCESS
        result1.output.contains('provider[registry.terraform.io/hashicorp/aws]')
    }

    void 'tfProvidersLock will lock more than the current platform'() {
        when: 'tfProvidersLock is run the first time'
        BuildResult result1 = nextRunner('tfProvidersLock').build()
        File lockFile = new File(srcDir,'.terraform.lock.hcl')

        then: 'the task is executed'
        result1.task(":tfProvidersLock").outcome == SUCCESS
        result1.output.contains('darwin_arm64')
        result1.output.contains('windows_386')
        lockFile.exists()
    }

    void 'tfProvidersSchema puts schema in an output file'() {
        when:
        BuildResult result1 = nextRunner('tfProvidersSchema').build()
        File schemaFile= new File(reportsDir,'main.schema.json')

        then:
        result1.task(":tfInit").outcome == SUCCESS
        result1.task(":tfProvidersSchema").outcome == SUCCESS
        schemaFile.exists()
    }

    void createTfSource() {
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
    }

    GradleRunner nextRunner(String... args) {
        List<String> args1 = [
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