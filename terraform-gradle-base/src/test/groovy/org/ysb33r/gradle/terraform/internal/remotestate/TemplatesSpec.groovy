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
package org.ysb33r.gradle.terraform.internal.remotestate

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.remotestate.BackendAttributesSpec
import org.ysb33r.gradle.terraform.remotestate.BackendTextTemplate
import org.ysb33r.grolifant.api.core.ProjectOperations
import spock.lang.Specification

class TemplatesSpec extends Specification {

    Project project = ProjectBuilder.builder().build()
    ProjectOperations projectOperations

    void setup() {
        projectOperations = ProjectOperations.maybeCreateExtension(project)
    }

    void 'Generate a configuration file from a text template'() {
        setup:
        def taskName = 'fooTask'
        def outputFile = project.provider { -> new File(project.projectDir, 'output.tf') }
        def attributes = new FakeAttributes()

        when:
        File target = Templates.generateFromTemplate(
            taskName,
            projectOperations,
            attributes,
            project.objects.property(File),
            project.provider { ->
                new TextTemplates.ReplaceTokens({ -> 'bucket = "@@bucket_name@@"' })
            } as Provider<BackendTextTemplate>,
            outputFile,
            '@@',
            '@@',
            attributes.tokens
        )

        then:
        target.exists()
        target.text.contains("bucket = \"${attributes.tokens.bucket_name}\"")
    }

    static class FakeAttributes implements BackendAttributesSpec {

        final String defaultTextTemplate = null

        final Provider<File> templateFile = null

        final Provider<BackendTextTemplate> textTemplate = null

        final Provider<Map<String, ?>> tokenProvider = null

        final Provider<String> beginTokenProvider = null

        final Provider<String> endTokenProvider = null

        @Override
        Map<String, Object> getTokens() {
            [
                aws_region       : 'REGION',
                remote_state_name: 'REMOTESTATE',
                bucket_name      : 'BUCKET'
            ]
        }
    }
}