package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.ExecSpec
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
        ExecSpec spec = task.buildExecSpec()
        spec.env.keySet().containsAll(["TF_DATA_DIR", "TF_CLI_CONFIG_FILE", "TF_LOG_PATH", "TF_LOG", "PATH", "HOME"])
        spec.env.size() == 6
        spec.args.containsAll(['-input=false', '-lock=true', '-lock-timeout=30s', '-parallelism=10', '-refresh=true','-detailed-exitcode'])
        spec.args.size() == 9
        def planfile = false
        spec.args.forEach{ it ->
            if(it.contains(".tf.plan")) {
                planfile = true
            }
        }
        def varsFile = false
        spec.args.forEach{ it ->
            if(it.contains("auto.tfvars.json")) {
                varsFile = true
            }
        }
        planfile == true
        varsFile == true
        // print(spec.getCommandLine())
    }
}
