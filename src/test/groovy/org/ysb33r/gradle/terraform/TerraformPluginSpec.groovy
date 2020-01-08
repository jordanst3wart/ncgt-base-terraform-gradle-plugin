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
import org.ysb33r.gradle.terraform.tasks.TerraformApply
import org.ysb33r.gradle.terraform.tasks.TerraformInit
import org.ysb33r.gradle.terraform.tasks.TerraformPlan
import spock.lang.Specification

class TerraformPluginSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Create additional source sets'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform'

        when:
        project.allprojects {
            terraformSourceSets {
                main {
                    srcDir = file('foo/bar')
                }
                release
            }
        }

        TerraformSourceSets tss = project.terraformSourceSets

        then:
        tss.getByName('main').srcDir.get() == project.file('foo/bar')
        tss.getByName( 'release').srcDir.get() == project.file('src/tf/release')
        project.tasks.getByName('tfReleaseInit') instanceof TerraformInit
        project.tasks.getByName('tfReleasePlan') instanceof TerraformPlan
        project.tasks.getByName('tfReleaseApply') instanceof TerraformApply
    }
}