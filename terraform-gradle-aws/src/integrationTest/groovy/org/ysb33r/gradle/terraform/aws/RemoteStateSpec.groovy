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
package org.ysb33r.gradle.terraform.aws

import org.ysb33r.gradle.terraform.aws.testfixtures.IntegrationSpecification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class RemoteStateSpec extends IntegrationSpecification {

    String taskName = 'createTfBackendConfiguration'

    void 'Create configuration file on GradleVersion == #gradleVersion'() {
        setup:
        new File(projectDir, 'src/tf/main').mkdirs()
        new File(projectDir, 'src/tf/main/main.tf').text = ''
        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform'
            id 'org.ysb33r.terraform.aws'
        }
        
        terraformSourceSets {
            main {
                workspaces 'production'
                aws {
                    usePropertiesForAws(
                        'production', 
                        provider { '123' }, 
                        provider { '456' }
                    )
                }
            }
        }
        
        createTfBackendConfiguration.textTemplate = 'not-empty'
        '''

        when:
        def result = getGradleRunner(true, projectDir, ['-i', taskName])
            .withGradleVersion(gradleVersion).build()

        then:
        result.task(":${taskName}").outcome == SUCCESS

        where:
        gradleVersion << ['4.10.3', '5.6.3']
    }
}