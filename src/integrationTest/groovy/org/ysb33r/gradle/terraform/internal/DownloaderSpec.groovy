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
//
// (C) Copyright Schalk W. Cronje 2017-2019
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
package org.ysb33r.gradle.terraform.internal

import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import org.ysb33r.grolifant.api.core.ProjectOperations
import spock.lang.IgnoreIf
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files

@RestoreSystemProperties
class DownloaderSpec extends DownloadTestSpecification {

    Project project = ProjectBuilder.builder().build()
    ProjectOperations projectOperations
    void setup() {
        projectOperations = ProjectOperations.create(project)
    }

    @IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
    def 'Download a Terraform distribution'() {
        given: 'A requirement to download Terraform #TERRAFORM_VERSION'
        Downloader dwn = new Downloader(DownloadTestSpecification.TERRAFORM_VERSION, projectOperations)
        dwn.downloadRoot = new File(project.buildDir, 'download')

        when: 'The distribution root is requested'
        File gotIt = dwn.distributionRoot

        String binaryName = DownloadTestSpecification.OS.windows ? 'terraform.exe' : 'terraform'

        then: 'The distribution is downloaded and unpacked'
        gotIt != null
        new File(gotIt, binaryName).exists()

        and: 'The executable has executable rights'
        Files.isExecutable(gotIt.toPath())

        when: 'The executable is run to display the help page'
        OutputStream output = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            executable dwn.terraformExecutablePath
            args '--help'
            standardOutput output
        }

        then: 'No runtime error is expected'
        result.assertNormalExitValue()

        and: 'The expected help information is displayed'
        output.toString().startsWith('''Usage: terraform [-version] [-help] <command> [args]''')
    }
}