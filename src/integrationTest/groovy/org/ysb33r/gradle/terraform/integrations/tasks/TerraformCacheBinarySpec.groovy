/*
 * Copyright 2017-2020 the original author or authors.
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
package org.ysb33r.gradle.terraform.integrations.tasks

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import org.ysb33r.gradle.terraform.integrations.IntegrationSpecification
import org.ysb33r.gradle.terraform.tasks.TerraformCacheBinary
import org.ysb33r.grolifant.api.core.OperatingSystem
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.ysb33r.gradle.terraform.plugins.TerraformWrapperPlugin.CACHE_BINARY_TASK_NAME

@IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
@RestoreSystemProperties
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
        loadProps(props, propsFile)

        then:
        result.task(":${CACHE_BINARY_TASK_NAME}").outcome == SUCCESS
        verifyAll {
            props.APP_LOCATION.endsWith(OS.windows ? 'terraform.exe' : 'terraform')
            props.APP_VERSION == terraformVersion
            props.USE_GLOBAL_CONFIG == 'false'
            props.CONFIG_LOCATION == new File(projectCacheDir, '.terraformrc').canonicalPath
        }
    }

    void loadProps(Properties props, File propsFile) {
        if (OperatingSystem.current().windows) {
            String contents = propsFile.text
                .replaceAll('set ', '') // Remove leading set keyword
            .replaceAll('@rem ', '# ') // Switch comment style
                .replaceAll('\\\\', '\\\\\\\\') // Replace \ with \\
                .replaceAll(':', '\\\\:') // Replace : with \:
                .replaceFirst('="', '=') // Strip leading quote
                .replaceAll(~/(?m)"$/, '') // Strip final quote
            new StringReader(contents).withReader { reader ->
                props.load(reader)
            }
        } else {
            propsFile.withReader { reader ->
                props.load(reader)
            }
        }
    }
}