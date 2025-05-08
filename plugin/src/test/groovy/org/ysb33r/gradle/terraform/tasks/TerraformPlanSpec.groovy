package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TerraformPlanSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'commands for terraform plan'() {
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
