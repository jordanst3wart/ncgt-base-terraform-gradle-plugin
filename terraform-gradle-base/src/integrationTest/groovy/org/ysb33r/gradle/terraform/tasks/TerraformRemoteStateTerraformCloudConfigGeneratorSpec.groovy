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
import org.ysb33r.gradle.terraform.testfixtures.ConfigGeneratorSpecification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TerraformRemoteStateTerraformCloudConfigGeneratorSpec extends ConfigGeneratorSpecification {
    void setup() {
        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform.remotestate.terraformcloud'
        }
        
        // tag::remote_state[]
        terraform { // <1>
            remote {
                // end::remote_state[]
                prefix = 'foo'
                // tag::remote_state[]
                terraformCloud { // <2>
                    hostname = 'foo.bar' // <3>
                    authToken = '123-456' // <4>
                    workspaceName = 'myWorkspace' // <5>
                }
            }
        }
        // end::remote_state[]
        '''
    }

    void 'Create Terraform Cloud configuration file with workspace prefix'() {
        setup:
        buildFile << '''
        // tag::remote_state2[]
        terraformSourceSets { // <6>
          main {
            remote {
              terraformCloud {
                workspacePrefix = 'ws' // <7>
              }
            }
          }
        }
        // end::remote_state2[]
        '''
        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS

        when:
        def lines = outputFile.readLines()

        then:
        verifyAll {
            lines.contains('hostname   = "foo.bar"')
            lines.contains('token      = "123-456"')
            lines.contains('workspaces = {"prefix" = "ws"}')
        }
    }

    void 'Create Terraform Cloud configuration file with workspace name'() {
        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS

        when:
        def lines = outputFile.readLines()

        then:
        verifyAll {
            lines.contains('hostname   = "foo.bar"')
            lines.contains('token      = "123-456"')
            lines.contains('workspaces = {"name" = "myWorkspace"}')
        }
    }
}