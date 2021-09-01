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
package org.ysb33r.gradle.terraform

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import spock.lang.Issue
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

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/-/issues/63')
    void 'Terraform extension on task honours variable providers on source set'() {
        setup:
        Action<VariablesSpec> variableProvider = { VariablesSpec vars ->
            vars.map([ foo : 123 ], 'remote_state')
        }

        when: 'a variable is provided via provider'
        project.allprojects {
            apply plugin : 'org.ysb33r.terraform'

            terraform {
                variables {
                    var 'project', '1'
                }
            }

            tfPlan {
                terraform {
                    variables {
                        var 'task', '2'
                    }
                }
            }
            terraformSourceSets {
                main {
                    variables {
                        var 'sourceSet', '3'
                    }
                }
            }

            terraform.variables.provider(variableProvider)
        }

        Variables vars = project.tasks.tfPlan.terraform.allVariables

        then: 'the task extension on tfPlan must make the provided values available'
        vars.escapedVars.remote_state

        and: 'the variables declared at project level'
        vars.escapedVars.project

        and: 'the variables declared at task level'
        vars.escapedVars.task

        and: 'the variable declared at source set'
        vars.escapedVars.sourceSet
    }
}