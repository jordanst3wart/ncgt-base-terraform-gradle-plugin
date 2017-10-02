package org.ysb33r.gradle.terraform.helpers


import TerraformInit

class IntegrationTestSpecification extends IntegrationTestBaseSpecification {

    TerraformInit terraformInitTask

    void setup() {
        project.apply plugin : 'org.ysb33r.terraform'
        terraformInitTask = project.tasks.getByName('terraformInit')
    }


    void processTerraformSource() {
        project.tasks.getByName('processTerraformSource').execute()
    }

    void initTerraformProject() {
        processTerraformSource()
        terraformInitTask.execute()
    }

    void copyTerraformFiles(final String sourceSubDir, final String destinationSubDir = 'src/terraform') {
        final File sourceDir = new File(DownloadTestSpecification.RESOURCES_DIR,sourceSubDir).absoluteFile
        final File destinationDir = project.file(destinationSubDir)
        destinationDir.mkdirs()
        project.copy {
            from sourceDir, {
                include '*.tf'
                include '*.tfvars'
                include '*.json'
            }
            into destinationDir
        }
    }
}