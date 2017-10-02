package org.ysb33r.gradle.terraform.integrations.tasks

import GradleException
import IntegrationTestSpecification
import TerraformPlan

class TerraformValidateIntegrationSpec extends IntegrationTestSpecification {

    TerraformPlan task

    void setup() {
        task = project.tasks.create('terraformValidate',TerraformPlan)

        project.allprojects {
            dependencies {
                terraform terraformProvider('local')
            }

            terraformValidate {
                source {
                    generatedBy 'processTerraformSource'
                }
            }
        }
    }

    def 'A proper TF file should not fail the task'() {
        setup:
        copyTerraformFiles('local-operations')

        when:
        project.evaluate()
        initTerraformProject()
        task.execute()

        then:
        noExceptionThrown()
    }


    def 'A bad TF file should fail the task'() {
        setup:
        copyTerraformFiles('broken')

        when:
        project.evaluate()
        initTerraformProject()
        task.execute()

        then:
        thrown(GradleException)
    }
}