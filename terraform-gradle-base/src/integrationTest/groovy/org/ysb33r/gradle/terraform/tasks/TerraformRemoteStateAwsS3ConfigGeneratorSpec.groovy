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

class TerraformRemoteStateAwsS3ConfigGeneratorSpec extends ConfigGeneratorSpecification {
    void setup() {
        buildFile.text = '''
        plugins {
            id 'org.ysb33r.terraform.remotestate.s3'
        }
        
        terraform {
            remote {
                prefix = 'foo'

                s3 {
                    bucket = 'bucket'
                    region = 'xx-north-0'
                }
            }
        }
        '''
    }

    void 'Create S3 configuration file from default template'() {
        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS

        when:
        def lines = outputFile.readLines()

        then:
        verifyAll {
            lines.contains('bucket = "bucket"')
            lines.contains('key    = "foo.tfstate"')
            lines.contains('region = "xx-north-0"')
        }
    }

    void 'Create S3 configuration file from all tokens'() {
        setup:
        buildFile << '''
        terraform {
            remote {
                s3 {
                    allTokenTemplate()
                }
            }
        }
        
        terraformSourceSets {
          main {
            remote {
              s3 {
                workspaceKeyPrefix = 'ws'
              }
            }
          }
        }
        '''
        when:
        BuildResult result = gradleRunner.build()

        then:
        result.task(":${taskName}").outcome == SUCCESS

        when:
        def lines = outputFile.readLines()

        then:
        verifyAll {
            lines.contains('bucket               = "bucket"')
            lines.contains('key                  = "foo.tfstate"')
            lines.contains('region               = "xx-north-0"')
            lines.contains('workspace_key_prefix = "ws"')
        }
    }
}