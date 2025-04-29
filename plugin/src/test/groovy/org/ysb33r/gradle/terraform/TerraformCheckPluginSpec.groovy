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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TerraformCheckPluginSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'The check task depends on tf*FmtCheck tasks'() {
        setup:
        project.apply plugin: 'bot.stewart.terraform'

        when:
        project.allprojects {
            terraformSourceSets {
                main {
                    srcDir = file('foo/bar')
                }
            }
        }

        def check = project.tasks.named('check').get()
        def tfFmtCheckProvider = project.tasks.named('fmtCheckMain').get()
        def tfFmtCheck = project.tasks.named('fmtCheckMain').get()

        def dependsOn = check.dependsOn.flatten()

        then:
        dependsOn.contains(tfFmtCheck.name) ||
            dependsOn.contains(tfFmtCheck) ||
            dependsOn.contains(tfFmtCheckProvider)
    }
}