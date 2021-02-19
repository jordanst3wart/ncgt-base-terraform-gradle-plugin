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
package org.ysb33r.gradle.terraform

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TerraformExtensionSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    def 'Configure terraform executable using a version'() {
        when: 'A version is configured'
        project.allprojects {
            apply plugin : 'org.ysb33r.terraform.base'

            // tag::configure-with-tag[]
            terraform {
                executable version : '1.0.3' // <1>
            }
            // end::configure-with-tag[]
        }

        then:
        project.terraform.resolvableExecutable != null
    }

    def 'Configure terraform executable using a path'() {
        when: 'A path is configured'
        project.allprojects {
            apply plugin : 'org.ysb33r.terraform.base'

            // tag::configure-with-path[]
            terraform {
                executable path : '/path/to/terraform' // <2>
            }
            // end::configure-with-path[]
        }

        then:
        project.terraform.resolvableExecutable != null
    }

    def 'Configure terraform executable using a search path'() {
        when: 'A search is configured'
        project.allprojects {
            apply plugin : 'org.ysb33r.terraform.base'

            // tag::configure-with-search-path[]
            terraform {
                executable searchPath() // <3>
            }
            // end::configure-with-search-path[]
        }

        then:
        project.terraform.resolvableExecutable != null
    }

    def 'Cannot configure terraform with more than one option'() {
        setup:
        project.apply plugin : 'org.ysb33r.terraform.base'

        when:
        project.terraform.executable version : '7.10.0', path : '/path/to'

        then:
        thrown(GradleException)

        when:
        project.terraform.executable version : '7.10.0', search : '/path/to'

        then:
        thrown(GradleException)
    }
}