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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TerraformPlanSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'commands for terraform plan'() {
        setup:
        project.apply plugin: 'bot.stewart.terraform'


        when:
        project.allprojects {
            terraformSourceSets {
                main {
                    srcDir = file('foo/bar')
                    variables {
                        file('auto.tfvars.json')
                    }
                }
            }
        }

        then:
        def task = project.tasks.named('planMain').get()
        task instanceof TerraformPlan
        // applyTask.setTargets(["someResource"]) // bug
        def spec = task.buildExecSpec()
        spec.getEnvironment().keySet().containsAll(["TF_DATA_DIR", "TF_CLI_CONFIG_FILE", "TF_LOG_PATH", "TF_LOG", "PATH", "HOME"])
        spec.getEnvironment().size() == 6
        spec.getCmdArgs().containsAll(['-input=false', '-lock=true', '-lock-timeout=30s', '-parallelism=10', '-refresh=true'])
        spec.getCmdArgs().size() == 8
        def planfile = false
        spec.getCmdArgs().forEach{ it ->
            if(it.contains(".tf.plan")) {
                planfile = true
            }
        }
        def varsFile = false
        spec.getCmdArgs().forEach{ it ->
            if(it.contains("auto.tfvars.json")) {
                varsFile = true
            }
        }
        planfile == true
        varsFile == true
        // print(spec.getCommandLine())
    }
}
