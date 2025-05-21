package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.ExecSpec
import spock.lang.Specification

class TerraformApplySpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'commands for terraform apply'() {
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
        def applyTask = project.tasks.named('applyMain').get()
        applyTask instanceof TerraformApply
        ExecSpec spec = applyTask.buildExecSpec()
        spec.env.keySet().containsAll(["TF_DATA_DIR", "TF_CLI_CONFIG_FILE", "TF_LOG_PATH", "TF_LOG", "PATH", "HOME"])
        spec.env.size() == 6
        spec.args.containsAll(['-auto-approve', '-input=false', '-lock=true', '-lock-timeout=30s', '-parallelism=10', '-refresh=true'])
        spec.args.size() == 7
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
        varsFile == false
        // print(spec.getCommandLine())
    }
}
