/*
 * Copyright 2017-2021 the original author or authors.
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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.credentials.SessionCredentials
import spock.lang.Specification

class TerraformAwsPluginSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Add aws extention to source set and use fixed credentials'() {
        setup:
        def awsKey = 'abcdefghijkl'
        def awsSecret = 'mnopqrstu'
        def awsKeyProvider = project.provider { -> awsKey }
        def awsSecretProvider = project.provider { -> awsSecret }
        project.apply plugin: 'org.ysb33r.terraform.aws'

        when:
        project.allprojects {
            terraformSourceSets {
                main {
                    aws {
                        usePropertiesForAws(awsKeyProvider, awsSecretProvider)
                    }
                }
            }
        }

        TerraformSourceSets tss = project.terraformSourceSets
        TerraformSourceDirectorySet main = tss.getByName('main')
        Set<SessionCredentials> set = main.credentialProviders.get()

        then:
        set.size() == 1
        !set.first().expired
        set.first().environment == [AWS_ACCESS_KEY_ID: awsKey, AWS_SECRET_ACCESS_KEY: awsSecret]
        set.first().refresh() == set.first()
    }
}