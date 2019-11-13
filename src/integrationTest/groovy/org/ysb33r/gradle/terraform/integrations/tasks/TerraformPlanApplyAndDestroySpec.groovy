/*
 * Copyright 2017-2019 the original author or authors.
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
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.IgnoreIf
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformPlanApplyAndDestroySpec extends IntegrationSpecification {

    public static final String FILE_CONTENTS = 'foo!!'

    File srcDir
    File destFile

    void setup() {
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        destFile = createTF()

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
            }
        }

        tfApply {
            logLevel = 'DEBUG'
        }
        """

    }

    @Unroll
    void 'Run terraform plan on a local resource (#state)'() {
        setup:
        File planFile = new File(buildDir, 'reports/tf/main/main.tf.plan')
        File textFile = new File(buildDir, "reports/tf/main/main.tf.plan.${json ? 'json' : 'txt'}")
        def cmdLine = json ? ['--json'] : []
        if (destroy) {
            buildFile.withWriterAppend { w ->
                w.println 'tfPlan.destructionPlan = true'
            }
        }

        when:
        BuildResult result = getGradleRunner(['tfPlan'] + cmdLine).build()

        then:
        result.task(":tfInit").outcome == SUCCESS
        planFile.exists()
        textFile.exists()

        where:
        destroy | state     | json
        false   | 'normal'  | false
        true    | 'destroy' | true
    }

    void 'Run terraform apply on a local resource'() {
        when:
        BuildResult result = getGradleRunner(['tfApply']).build()

        then:
        result.task(":tfApply").outcome == SUCCESS
        destFile.text == FILE_CONTENTS
    }

    void 'Run terraform destroy on a local resource'() {
        when:
        BuildResult result = getGradleRunner(['tfApply', 'tfDestroy', '--approve']).build()

        then:
        result.task(":tfDestroy").outcome == SUCCESS
        !destFile.exists()
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
        new File(srcDir, 'init.tf').text = """
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