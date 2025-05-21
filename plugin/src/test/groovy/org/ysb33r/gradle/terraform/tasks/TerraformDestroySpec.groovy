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
import org.ysb33r.gradle.terraform.ExecSpec
import spock.lang.Specification

class TerraformDestroySpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'commands for terraform destroy'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'


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
        def task = project.tasks.named('destroyMain').get()
        task instanceof TerraformDestroy
        // task.setTargets(["someResource"]) // bug
        ExecSpec spec = task.buildExecSpec()
        spec.env.keySet().containsAll(["TF_DATA_DIR", "TF_CLI_CONFIG_FILE", "TF_LOG_PATH", "TF_LOG", "PATH", "HOME"])
        spec.env.size() == 6
        spec.args.containsAll([
            '-auto-approve',
            '-input=false',
            '-lock=true',
            '-lock-timeout=30s',
            '-parallelism=10',
            '-refresh=true',
        ])
        spec.args.size() == 8

        def varsFile = false
        spec.args.forEach{ it ->
            if(it.contains("auto.tfvars.json")) {
                varsFile = true
            }
        }
        varsFile == true
        // NOTE can't use a plan file
        /*
        def planfile = false
        spec.getCmdArgs().forEach{ it ->
            if(it.contains(".tf.destroy.plan")) {
                planfile = true
            }
        }
        planfile == true
        */
        // print(spec.getCommandLine())
    }
}
