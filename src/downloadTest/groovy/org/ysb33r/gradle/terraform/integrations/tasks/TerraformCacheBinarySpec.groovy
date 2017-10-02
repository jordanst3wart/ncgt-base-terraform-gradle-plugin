package org.ysb33r.gradle.terraform.integrations.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import org.ysb33r.gradle.terraform.integrations.IntegrationSpecification
import org.ysb33r.gradle.terraform.tasks.TerraformCacheBinary
import spock.lang.IgnoreIf

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.gradle.terraform.plugins.TerraformWrapperPlugin.CACHE_BINARY_TASK_NAME

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
class TerraformCacheBinarySpec extends IntegrationSpecification {

    void 'Write cache file from version'() {
        setup:
        String terraformVersion = TerraformExtension.TERRAFORM_DEFAULT
        File gradleUserHomeDir = new File(projectDir, '.USERHOME')

        GradleRunner gradleRunner = getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                CACHE_BINARY_TASK_NAME,
                '-s',
                '-g',
                gradleUserHomeDir.absolutePath
            ]
        )
        File propsFile = new File(projectCacheDir, TerraformCacheBinary.LOCATION_PROPERTIES_DEFAULT)
        Properties props = new Properties()

        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform.wrapper'
        }
        '''

        when:
        BuildResult result = gradleRunner.build()
        propsFile.withReader { reader ->
            props.load(reader)
        }

        then:
        result.task(":${CACHE_BINARY_TASK_NAME}").outcome == SUCCESS
        verifyAll {
            props.location.endsWith('terraform')
            props.binaryVersion == terraformVersion
        }
    }
}