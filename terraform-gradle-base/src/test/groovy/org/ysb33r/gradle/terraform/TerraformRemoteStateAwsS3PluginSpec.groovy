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
package org.ysb33r.gradle.terraform

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.gradle.terraform.remotestate.LocalBackendSpec
import org.ysb33r.gradle.terraform.remotestate.RemoteStateS3Spec
import org.ysb33r.gradle.terraform.remotestate.TerraformRemoteStateExtension
import org.ysb33r.gradle.terraform.tasks.RemoteStateConfigGenerator
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.core.StringTools
import spock.lang.Specification

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.backendTaskName

class TerraformRemoteStateAwsS3PluginSpec extends Specification {

    public static final String PLUGIN_ID = 'org.ysb33r.terraform.remotestate.s3'
    public static final String SOURCE_SET_NAME = 'main'

    Project project = ProjectBuilder.builder().build()
    ProjectOperations po
    StringTools stringTools
    TerraformRemoteStateExtension remote
    RemoteStateS3Spec s3
    LocalBackendSpec local
    TerraformRemoteStateExtension sourceSetRemote
    RemoteStateS3Spec sourceSetS3
    LocalBackendSpec sourceSetLocal
    RemoteStateConfigGenerator generatorTask

    void setup() {
        project.apply plugin: PLUGIN_ID
        po = ProjectOperations.maybeCreateExtension(project)
        stringTools = po.stringTools
        remote = TerraformRemoteStateExtension.findExtension(project)
        s3 = RemoteStateS3Spec.findExtension(project)
        sourceSetRemote = TerraformRemoteStateExtension.findExtension(project, SOURCE_SET_NAME)
        sourceSetS3 = RemoteStateS3Spec.findExtension(project, SOURCE_SET_NAME)
        local = LocalBackendSpec.findExtension(project, LocalBackendSpec)
        sourceSetLocal = LocalBackendSpec.findExtension(project, SOURCE_SET_NAME, LocalBackendSpec)
        generatorTask = project.tasks.getByName(backendTaskName(SOURCE_SET_NAME))
    }

    void 'Plugin is applied'() {
        expect: 'Default tasks are created'
        project.tasks.getByName('createTfBackendConfiguration')
        project.extensions.getByName('terraform')
        project.terraform.extensions.getByName('remote')
        project.terraform.remote.extensions.getByName('s3')

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
        project.tasks.getByName('createTfBackendConfiguration')
    }

    void 'Configuring terraform.remote.s3 sets properties on task'() {
        setup:
        def region = 'blah-blah'
        def bucket = 'car'
        NamedDomainObjectContainer<TerraformSourceDirectorySet> tss = project.terraformSourceSets

        when: 's3 is globally configured for region and bucket'
        sourceSetRemote.follow(remote)
        s3.region = region
        s3.bucket = bucket

        and: 'remote_state variable is requested'
        tss.getByName('main').remote.remoteStateVar = true

        and: 'obtaining tokens from various sources'
        def taskTokens = stringTools.stringizeValues(project.tasks.createTfBackendConfiguration.tokens)
        def globalSpecTokens = stringTools.stringizeValues(s3.tokens)
        def tssSpecTokens = stringTools.stringizeValues(tss.getByName('main').remote.s3.tokens)

        then: 'the task will pick up appropriate aws configuration'
        taskTokens['bucket'] == bucket
        taskTokens['region'] == region
        taskTokens.keySet().containsAll(globalSpecTokens.keySet())
        taskTokens.keySet().containsAll(tssSpecTokens.keySet())

        when: 'the variables are analysed'
        Map<String, String> vars = ((Variables) tss.getByName('main').variables).escapedVars

        then: 'remote_state map will be passed to terraform'
        vars.remote_state
    }

    void 'The default destination directory is based upon the source set name'() {
        setup:
        project.terraformSourceSets {
            additional {
            }
        }

        File main = outputFile(project.tasks.createTfBackendConfiguration)
        File additional = outputFile(project.tasks.createTfAdditionalBackendConfiguration)

        expect:
        main.name == 'terraform-backend-config.tf'
        main.parentFile.name == 'tfBackendConfiguration'
        additional.parentFile.name == 'tfAdditionalBackendConfiguration'
        main.parentFile.parentFile == new File(project.buildDir, 'tfRemoteState')
        project.tasks.createTfBackendConfiguration.destinationDir.get() == main.parentFile
    }

    void 'Tokens for the template can be configured'() {
        when: 'the plugin is applied'
        RemoteStateConfigGenerator task = project.tasks.createTfBackendConfiguration

        then: 'there are some default tokens set'
        task.tokens.keySet().containsAll(['key'])

        when: 'this tokens are replaced and added to'
        s3.tokens = [foo: 1]
        s3.tokens bar: 2

        then: 'then the new set should be available'
        task.tokens.keySet().containsAll(['foo', 'bar'])
    }

    void 'terraformInit has configuration file correctly setup'() {
        expect:
        project.tasks.tfInit.backendConfigFile.get() == outputFile(project.tasks.createTfBackendConfiguration)
    }

    void 'Extensions are added to terraform source directory sets'() {
        expect:
        project.terraformSourceSets.getByName('main').remote.s3 instanceof RemoteStateS3Spec
    }

    void 'By default S3 backend on source set follows S3 backend on global'() {
        when:
        s3.assumeRoleDurationSeconds = 12345

        then:
        allTokens['assume_role_duration_seconds'] == '12345'
    }

    void 'If no templates were set on source set S3 backend then global templates are used unless nofollow is set'() {
        expect:
        s3.textTemplate.present
        !sourceSetS3.textTemplate.present
        sourceSetRemote.textTemplate.get() == s3.textTemplate.get()
    }

    void 'Setting a template on source set will ignore further global template changes'() {
        when:
        sourceSetS3.textTemplate = 'abc'
        s3.textTemplate = 'def'

        then:
        s3.textTemplate.get() != sourceSetS3.textTemplate.get()
    }

    void 'Changing backend on the source set to local uses the global settings for local unless nofollow is set'() {
        when:
        sourceSetRemote.backend = LocalBackendSpec
        local.path = 'foobar'

        then:
        allTokens['path'] == project.file('foobar').absolutePath
    }

    void 'Global S3 backend can be unfollowed'() {
        when:
        sourceSetRemote.noFollow()
        s3.assumeRoleDurationSeconds = 12345

        then:
        s3.tokens.containsKey('assume_role_duration_seconds')
        !sourceSetS3.tokens.containsKey('assume_role_duration_seconds')
    }

    private File outputFile(RemoteStateConfigGenerator task) {
        task.backendConfigFile.get()
    }

    private Map<String, String> getAllTokens() {
        po.stringTools.stringizeValues(sourceSetRemote.tokenProvider.get())
    }
}