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
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformPlanApplyAndDestroySpec extends IntegrationSpecification {

    public static final String FILE_CONTENTS = 'foo!!'

    File srcDir
    File terraformApplyOutputFile
    File terraformSourceFile

    void setup() {
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        terraformSourceFile = new File(srcDir, 'init.tf')
        terraformApplyOutputFile = createTF()

        String path = terraformApplyOutputFile.canonicalPath

        buildFile.text = """
        plugins {
            id 'org.ysb33r.terraform'
        }
        
        terraformSourceSets {
            main {
                variables {
                    var 'foofile', '${DownloadTestSpecification.OS.windows ? path.replaceAll(~/\x5C/, '/') : path}'
                }
            }
        }
        """
    }

    @Unroll
    void 'Run terraform plan on a local resource (#format)'() {
        setup:
        File planFile = new File(buildDir, 'tf/main/main.tf.plan')
        File textFile = new File(buildDir, "reports/tf/main/main.tf.plan.${json ? 'json' : 'txt'}")
        def cmdLine = json ? ['--json'] : []

        when:
        BuildResult result = getGradleRunner(['tfPlan'] + cmdLine).build()

        then:
        result.task(':tfInit').outcome == SUCCESS
        planFile.exists()
        textFile.exists()

        where:
        format | json
        'text' | false
        'json' | true
    }

    @Unroll
    void 'Create destroy plan (#format)'() {
        setup:
        File planFile = new File(buildDir, 'tf/main/main.tf.destroy.plan')
        File textFile = new File(buildDir, "reports/tf/main/main.tf.destroy.plan.${json ? 'json' : 'txt'}")
        def cmdLine = json ? ['--json'] : []

        when:
        BuildResult result = getGradleRunner(['tfApply', 'tfDestroyPlan'] + cmdLine).build()

        then:
        result.task(':tfApply').outcome == SUCCESS
        result.task(':tfDestroyPlan').outcome == SUCCESS
        planFile.exists()
        textFile.exists()

        where:
        format | json
        'text' | false
        'json' | true
    }

    void 'Run terraform apply on a local resource'() {
        when:
        BuildResult result = getGradleRunner(['tfApply']).build()

        then:
        result.task(':tfApply').outcome == SUCCESS
        terraformApplyOutputFile.text == FILE_CONTENTS
    }

    void 'Run terraform state pull after apply'() {
        given:
        File stateFile = new File(srcDir, 'foo.tfstate')

        when:
        BuildResult result = getGradleRunner(['tfApply', 'tfStatePull', '--state-file', 'foo.tfstate']).build()

        then:
        result.task(':tfStatePull').outcome == SUCCESS
        stateFile.exists()
    }

    void 'tfShowState should cause tfApply to be executed'() {
        setup:
        getGradleRunner(['tfApply']).build()

        when:
        BuildResult result = getGradleRunner(['tfShowState']).build()

        then:
        result.task(':tfShowState').outcome == SUCCESS
        result.task(':tfApply') == null

        when:
        BuildResult result2 = getGradleRunner(['tfShowState']).build()

        then:
        result2.task(':tfShowState').outcome == SUCCESS
    }

    void 'Plan should not run twice if nothing changed'() {
        when:
        BuildResult result = getGradleRunner(['tfPlan']).build()

        then:
        result.task(':tfPlan').outcome == SUCCESS

        when:
        BuildResult result2 = getGradleRunner(['tfApply']).build()

        then:
        result2.task(':tfInit').outcome == UP_TO_DATE
        result2.task(':tfPlan').outcome == UP_TO_DATE
        result2.task(':tfApply').outcome == SUCCESS

        when:
        BuildResult result3 = getGradleRunner(['tfApply']).build()

        then:
        result3.task(':tfInit').outcome == UP_TO_DATE
        result3.task(':tfPlan').outcome == UP_TO_DATE
        result3.task(':tfApply').outcome == UP_TO_DATE
    }

    void 'Run terraform destroy on a local resource'() {
        when:
        BuildResult result = getGradleRunner(['tfApply', 'tfDestroy', '--approve']).build()

        then:
        result.task(':tfDestroy').outcome == SUCCESS
        !terraformApplyOutputFile.exists()
    }

    void 'tfApply should not run again when tfDestroy is called'() {
        when: 'Infrastructure is applied'
        getGradleRunner(['tfApply']).build()

        and: 'Sources are modified'
        terraformApplyOutputFile << '\n\n\n'

        and: 'tfDestroy is called without tfApply preceding it'
        BuildResult result = getGradleRunner(['tfDestroy', '--approve']).build()

        then: 'Only tfDestroy should be executed'
        result.task(':tfDestroy').outcome == SUCCESS
        result.task(':tfApply') == null
        result.task(':tfPlan') == null
    }

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/-/issues/47')
    void 'tfApply should pass target and replace parameters to tfPlan'() {
        when: 'Infrastructure is applied'
        BuildResult result1 = getGradleRunner(['tfApply', '--target', 'local_file.foo']).build()

        then:
        result1.task(':tfPlan').outcome == SUCCESS
        result1.output.contains('-target=local_file.foo')

        when:
        BuildResult result2 = getGradleRunner(['tfApply', '--replace', 'local_file.foo']).build()

        then:
        result2.task(':tfPlan').outcome == SUCCESS
        result2.output.contains('-replace=local_file.foo')
    }

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/-/issues/63')
    void 'tfPlan should add remote_state to command-line if remoteStateVarProvider is set'() {
        setup:
        terraformSourceFile << '''
        variable remote_state {
            type = map(string)
        }
        '''

        buildFile << '''
        terraformSourceSets {
            main {
                remote {
                    remoteStateVar = true
                }
            }
        }
        '''

        when: 'Infrastructure is applied'
        BuildResult result1 = getGradleRunner(['tfPlan', '-i']).build()

        then:
        result1.task(':tfPlan').outcome == SUCCESS
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

    File createTF() {
        File destFile = new File(testProjectDir.root, 'TF/foo.bar')
        terraformSourceFile.text = """
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