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
package org.ysb33r.gradle.terraform.internal.remotestate

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.tasks.RemoteStateAwsS3ConfigGenerator
import spock.lang.Specification

class TemplatesSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Generate a configuration file from a default template'() {
        setup:
        def taskName = 'fooTask'
        def outputFile = project.provider { -> new File(project.projectDir, 'output.tf') }
        def tokens = [
            aws_region       : 'REGION',
            remote_state_name: 'REMOTESTATE',
            bucket_name      : 'BUCKET'
        ]

        when:
        File target = Templates.generateFromTemplate(
            taskName,
            project,
            RemoteStateAwsS3ConfigGenerator.TEMPLATE_RESOURCE_PATH,
            project.objects.property(File),
            outputFile,
            '@@',
            '@@',
            tokens
        )

        then:
        target.exists()
        target.text.contains("bucket = \"${tokens.bucket_name}\"")
    }
}