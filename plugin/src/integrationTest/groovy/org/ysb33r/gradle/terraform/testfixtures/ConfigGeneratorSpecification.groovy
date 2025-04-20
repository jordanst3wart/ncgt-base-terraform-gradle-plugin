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
package org.ysb33r.gradle.terraform.testfixtures

import org.gradle.testkit.runner.GradleRunner

class ConfigGeneratorSpecification extends IntegrationSpecification {
    public static final String PROJECT_NAME = 'test-project'
    String taskName = 'createTfBackendConfiguration'
    File testkitDir
    File srcDir
    File outputFile
    GradleRunner gradleRunner

    void setup() {
        testkitDir = testProjectDir.newFolder()
        srcDir = new File(projectDir, 'src/tf/main')
        srcDir.mkdirs()
        outputFile = new File(projectDir, 'build/tfRemoteState/tfBackendConfiguration/terraform-backend-config.tf')

        new File(projectDir, 'settings.gradle').text = "rootProject.name = '${PROJECT_NAME}'"

        gradleRunner = getGradleRunner(
            IS_GROOVY_DSL,
            projectDir,
            [
                taskName,
                '-i', '-s',
            ]
        ).withTestKitDir(testkitDir)
    }
}
