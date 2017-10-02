package org.ysb33r.gradle.terraform.integrations.tasks


import IntegrationTestSpecification
import TerraformInit

class TerraformInitIntegrationSpec extends IntegrationTestSpecification {

    void setup() {
        project.allprojects {
            // tag::adding-dependencies[]
            dependencies {
                terraform terraformProvider('aws')
                terraform terraformProvider('local')
            }
            // end::adding-dependencies[]
        }
    }

    def 'Plugin can resolve dependency listings'() {
        when:
        project.evaluate()
        Set<File> providers = terraformConfiguration.resolve()

        then:
        providers.size() == 2
        project.file('terraform.lock').exists()
    }

    def 'Initialising a TF file with a single provider'() {

        setup:
        TerraformInit task = terraformInitTask
        copyTerraformFiles('two-providers')

        project.allprojects {
            terraformInit {
                environment TF_LOG : 'TRACE'
            }
        }

        when:
        processTerraformSource()
        task.execute()

        then:
        noExceptionThrown()
        terraformExtension.getPluginCacheDir().listFiles().size() == 2
    }
}