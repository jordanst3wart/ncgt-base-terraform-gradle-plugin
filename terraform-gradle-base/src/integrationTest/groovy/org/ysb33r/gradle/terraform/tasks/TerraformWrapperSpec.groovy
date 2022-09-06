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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.testfixtures.DownloadTestSpecification
import org.ysb33r.gradle.terraform.testfixtures.IntegrationSpecification
import spock.lang.IgnoreIf
import spock.lang.Timeout
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.gradle.terraform.plugins.TerraformWrapperPlugin.WRAPPER_TASK_NAME

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
class TerraformWrapperSpec extends IntegrationSpecification {

    @Timeout(300)
    void 'Create wrapper'() {
        setup:
        String terraformVersion = TerraformExtension.TERRAFORM_DEFAULT
        File terraformw = new File(projectDir, 'terraformw')
        File terraformw_bat = new File(projectDir, 'terraformw.bat')

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
        StringWriter err = new StringWriter()
        StringWriter out = new StringWriter()
        File wrapper = DownloadTestSpecification.OS.windows ? terraformw_bat : terraformw
        List<String> envVars = DownloadTestSpecification.OS.windows ?
            ['DEBUG=1', "TEMP=${System.getenv('TEMP')}"] :
            ['DEBUG=1']
        Process runWrapper = "${wrapper.absolutePath} -version".execute(envVars, wrapper.parentFile)
        runWrapper.consumeProcessOutput(out, err)
        runWrapper.waitForOrKill(1000)
        int exitCode = runWrapper.exitValue()
        if (exitCode) {
            println '---[ stderr ]----------------------------------'
            println err
            println '---[ stdout ]----------------------------------'
            println out
            println '-----------------------------------------------'
        }

        then: 'The version should be printed'
        exitCode == 0
        out.toString().contains("Terraform v${terraformVersion}")
    }
}
