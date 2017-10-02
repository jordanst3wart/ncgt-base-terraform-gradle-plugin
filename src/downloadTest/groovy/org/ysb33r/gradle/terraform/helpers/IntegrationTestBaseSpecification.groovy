package org.ysb33r.gradle.terraform.helpers

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.plugins.TerraformBasePlugin

class IntegrationTestBaseSpecification extends DownloadTestSpecification {

    Project project = ProjectBuilder.builder().build()
    TerraformExtension terraformExtension
    Configuration terraformConfiguration

    void setupSpec() {
        final String prop = 'org.ysb33r.gradle.hashicorp.releases.uri'

        if(!System.getProperty(prop)) {
            System.setProperty(prop,DownloadTestSpecification.TERRAFORM_CACHE_DIR.toURI().toString())
        }
    }

    void setup() {
        project.apply plugin : 'org.ysb33r.terraform.base'
        terraformExtension = project.extensions.getByName(TerraformExtension.NAME)
        terraformConfiguration = project.configurations.getByName(TerraformBasePlugin.TERRAFORM_CONFIGURATION)
        project.allprojects {
            terraform {
                warnOnNewVersion false
            }
        }
    }

    void processTerraformSource() {
        project.tasks.getByName('processTerraformSource').execute()
    }

    void copyTerraformFiles(final String sourceSubDir, final String destinationSubDir = 'src/terraform') {
        final File sourceDir = new File(DownloadTestSpecification.RESOURCES_DIR,sourceSubDir).absoluteFile
        final File destinationDir = project.file(destinationSubDir)
        destinationDir.mkdirs()
        project.copy {
            from sourceDir, {
                include '*.tf'
                include '*.tfvars'
            }
            into destinationDir
        }
    }
}