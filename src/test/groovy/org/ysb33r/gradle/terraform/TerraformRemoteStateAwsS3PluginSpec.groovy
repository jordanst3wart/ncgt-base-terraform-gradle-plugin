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
package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator
import spock.lang.Specification

import static org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator.CONFIG_FILE_NAME

class TerraformRemoteStateAwsS3PluginSpec extends Specification {

    public static final String PLUGIN_ID = 'org.ysb33r.terraform.remotestate.s3'

    Project project = ProjectBuilder.builder().build()
    TerraformRemoteStateExtension remote
    RemoteStateS3 s3

    void setup() {
        project.apply plugin: PLUGIN_ID
        remote = TerraformRemoteStateExtension.findExtension(project)
        s3 = RemoteStateS3.findExtension(project)
    }

    void 'Plugin is applied'() {
        expect: 'Default tasks are created'
        project.tasks.getByName('createTfS3BackendConfiguration')

        and: 'Remote state name prefix is the project name by default'
        remote.prefix.get() == project.name
    }

    void 'Plugin is applied after "org.ysb33r.terraform" plugin'() {
        when:
        Project project2 = ProjectBuilder.builder().build()
        project2.apply plugin: 'org.ysb33r.terraform'
        project2.apply plugin: PLUGIN_ID

        then:
        noExceptionThrown()
        project.tasks.getByName('createTfS3BackendConfiguration')
    }

    void 'Tasks are created for additional source sets'() {
        setup:
        project.terraformSourceSets {
            additional {
            }
        }

        expect:
        project.tasks.getByName('createTfAdditionalS3BackendConfiguration')
    }

    void 'Configuring terraform.remote extension sets property on task'() {
        setup:
        def newPrefix = 'foo'

        when: 'An additional source set is added'
        project.terraformSourceSets {
            additional {
            }
        }

        and: 'remote is configured for remote state prefix'
        remote.prefix = newPrefix

        then: 'the tasks will pick up appropriate remote state names'
        project.tasks.createTfS3BackendConfiguration.remoteStateName.get() == newPrefix
        project.tasks.createTfAdditionalS3BackendConfiguration.remoteStateName.get() == "${newPrefix}-additional"
    }

    void 'Configuring terraform.remote.s3 sets properties on task'() {
        setup:
        def region = 'blah-blah'
        def bucket = 'car'

        when: 's3 is configured for region and bucket'
        s3.region = region
        s3.bucket = bucket

        then: 'the task will pick up appropriate aws configuration'
        project.tasks.createTfS3BackendConfiguration.s3BucketName.get() == bucket
        project.tasks.createTfS3BackendConfiguration.awsRegion.get() == region
    }

    void 'The default destination directory is based upon the source set name'() {
        setup:
        project.terraformSourceSets {
            additional {
            }
        }

        File main = outputFile(project.tasks.createTfS3BackendConfiguration)
        File additional = outputFile(project.tasks.createTfAdditionalS3BackendConfiguration)

        expect:
        main.name == CONFIG_FILE_NAME
        main.parentFile.name == 'tfS3BackendConfiguration'
        additional.parentFile.name == 'tfAdditionalS3BackendConfiguration'
        main.parentFile.parentFile == project.buildDir
        project.tasks.createTfS3BackendConfiguration.destinationDir.get() == main.parentFile
    }

    void 'Can customise the template'() {
        when: 'the plugin is applied'
        RemoteStateAwsS3ConfigGenerator task = project.tasks.createTfS3BackendConfiguration
        String defaultDelimiter = '@@'

        then: 'the token delimiters are "@@"'
        task.beginToken == defaultDelimiter
        task.endToken == defaultDelimiter
        !task.templateFile.present

        when: 'the template file and tokens are changed'
        task.delimiterTokenPair('##', '$$')
        task.templateFile = 'src/foo.tmpl'

        then: 'the new values are expected to be set'
        task.beginToken == '##'
        task.endToken == '$$'
        task.templateFile.get().name == 'foo.tmpl'
    }

    void 'Tokens for the template can be configured'() {
        when: 'the plugin is applied'
        RemoteStateAwsS3ConfigGenerator task = project.tasks.createTfS3BackendConfiguration

        then: 'there are some default tokens set'
        task.tokens.keySet().containsAll(['aws_region', 'remote_state_name', 'bucket_name'])

        when: 'this tokens are replaced and added to'
        task.tokens = [foo: 1]
        task.tokens bar: 2

        then: 'then the new set should be available'
        task.tokens.keySet().containsAll(['foo', 'bar'])
    }

    void 'terraformInit has configuration file corretly setup'() {
        expect:
        project.tasks.tfInit.backendConfigFile.get() == outputFile(project.tasks.createTfS3BackendConfiguration)
    }

    private File outputFile(RemoteStateAwsS3ConfigGenerator task) {
        task.backendConfigFile.get()
    }
}