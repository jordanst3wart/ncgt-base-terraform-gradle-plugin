package org.ysb33r.gradle.terraform.integrations.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import org.ysb33r.gradle.terraform.integrations.IntegrationSpecification
import org.ysb33r.gradle.terraform.tasks.TerraformCacheBinary
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.gradle.terraform.plugins.TerraformWrapperPlugin.CACHE_BINARY_TASK_NAME
import static org.ysb33r.gradle.terraform.plugins.TerraformWrapperPlugin.WRAPPER_TASK_NAME

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformWrapperSpec extends IntegrationSpecification {

    void 'Create wrapper'() {
        setup:
        String terraformVersion = TerraformExtension.TERRAFORM_DEFAULT
        File terraformw = new File(projectDir,'terraformw')
        File terraformw_bat = new File(projectDir,'terraformw.bat')

        GradleRunner gradleRunner = getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                'wrapper',
                WRAPPER_TASK_NAME,
                '-s'
            ]
        )

        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform.wrapper'
        }
        '''

        when: 'The terraform wrapper task is executed'
        BuildResult result = gradleRunner.build()

        then: 'Terraform wrapper scripts are generated'
        result.task(":${WRAPPER_TASK_NAME}").outcome == SUCCESS
        terraformw.exists()
        terraformw_bat.exists()

        when: 'The Terraform wrapper script is executed'
        File wrapper = OS.windows ? terraformw_bat : terraformw
        Process runWrapper = "${wrapper.absolutePath} -version".execute([],wrapper.parentFile)
        runWrapper.waitFor()

        then: 'The version should be printed'
        runWrapper.text.contains("Terraform v${terraformVersion}")
    }
}