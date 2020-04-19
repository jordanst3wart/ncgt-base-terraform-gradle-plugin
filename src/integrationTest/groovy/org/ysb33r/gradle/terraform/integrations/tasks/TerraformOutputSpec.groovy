/*
 * Copyright 2017-2020 the original author or authors.
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
import org.ysb33r.grashicorp.HashicorpUtils
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformOutputSpec extends IntegrationSpecification {

    String taskName = 'tfOutput'
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
                'tfInit',
                'tfApply',
                taskName,
                '-s',
            ]
        ).withTestKitDir(testkitDir)
    }

    void 'Run terraform output'() {
        setup:
        createTfSpec()

        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS
        new File(buildDir,'reports/tf/main/main.outputs.tf').exists()
    }

    void 'Access outputs as provider'() {
        setup:
        createTfSpec()
        buildFile << """
        task testOutput {
            doLast {
                assert terraformSourceSets.getByName('main').rawOutputVariables.get().arbitrary_value
                assert terraformSourceSets.getByName('main').rawOutputVariable('arbitrary_number').get() == 123
            }
        }    
        """
        getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'tfInit',
                'tfApply',
            ]
        ).withTestKitDir(testkitDir).build()

        when:
        BuildResult result = getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'testOutput',
                '-i','-s'
            ]
        ).withTestKitDir(testkitDir).build()

        then:
        result.task(":testOutput").outcome == SUCCESS
    }

    void createTfSpec() {
        new File(srcDir, 'init.tf').text = '''
        variable "foo" {
            type = string
            default = "bar"
        }

        variable "numeri" {
            type = number
            default = 123
        }
        
        output "arbitrary_value" {
            value = var.foo
        }
        output "arbitrary_number" {
            value = var.numeri
        }
        '''
    }
}