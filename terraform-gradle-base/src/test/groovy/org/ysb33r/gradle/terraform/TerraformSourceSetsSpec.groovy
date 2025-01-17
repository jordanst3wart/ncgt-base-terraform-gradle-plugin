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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import spock.lang.Issue
import spock.lang.Specification

class TerraformSourceSetsSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/issues/1')
    void 'Variable definitions in source set should not create new source sets'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform'
        def varAction = new Action<VariablesSpec>() {
            @Override
            void execute(VariablesSpec vs) {
                vs.var 'foo2', 'bar2'
            }
        }

        when:
        project.allprojects {
            ext {
                myStr1 = 'bar1'
            }

            terraformSourceSets {
                main {
                    variables {
                        var 'foo1', myStr1
                    }

                    variables varAction
                }
            }
        }

        NamedDomainObjectContainer<TerraformSourceDirectorySet> tss = project.terraformSourceSets
        def allVars = ((Variables) tss.getByName('main').variables).allVars

        then:
        verifyAll {
            !tss.getByName('main').hasWorkspaces()
            allVars.vars.foo1 == 'bar1'
            allVars.vars.foo2 == 'bar2'
        }
    }

    void 'Items must be able resolve entities in project scope'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform'
        project.apply plugin: 'org.ysb33r.terraform.remotestate.s3'
        project.extensions.create('testExt', TestExtension, project.providers, project)

        when:
        configureFourSourceSets()

        then:
        noExceptionThrown()
    }

    void 'Items must be able resolve entities in project scope even with different order of plugins applied'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform.remotestate.s3'
        project.apply plugin: 'org.ysb33r.terraform'
        project.extensions.create('testExt', TestExtension, project.providers, project)

        when:
        configureFourSourceSets()

        then:
        noExceptionThrown()
    }

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/-/issues/34')
    void 'Can add workspaces'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform'

        when:
        project.allprojects {
            terraformSourceSets {
                main {
                    workspaces 'alpha', 'beta', 'gamma'
                }
            }
        }

        TerraformSourceSets tss = project.terraformSourceSets
        def main = tss.getByName('main')

        then:
        main.hasWorkspaces()
        main.workspaceNames.size() == 3
    }

    void 'Can provide a tfvars file'() {
        setup:
        project.apply plugin: 'org.ysb33r.terraform'

        project.allprojects {
            terraformSourceSets {
                main {
                    variables {
                        file 'foo.tfvars'
                        file 'foo2.tfvars'
                    }
                }
            }
        }

        when:
        Variables vars = project.terraformSourceSets.getByName('main').variables
        final cmdline = vars.commandLineArgs
        final fooPos = cmdline.findIndexOf { it.endsWith('foo.tfvars') }
        final foo2Pos = cmdline.findIndexOf { it.endsWith('foo2.tfvars') }

        then:
        vars.fileNames.contains('foo.tfvars')
        cmdline.contains("-var-file=${project.file('src/tf/main/foo.tfvars')}".toString())

        and:
        fooPos < foo2Pos
    }

    void configureFourSourceSets() {
        project.allprojects {
            terraformSourceSets {
                main {
                    remote {
                        s3 {
                        }
                    }
                }
                create('created') {
                    remote {
                        s3 {
                        }
                    }
                }
                register('registered') {
                    remote {
                        s3 {
                        }
                    }
                }
                groovyAutoAddStyle {
                    remote {
                        s3 {
                        }
                    }
                }
            }
        }
    }

    static class TestExtension {
        final ProviderFactory providers
        final Project project1

        TestExtension(ProviderFactory p, Project p1) {
            this.providers = p
            this.project1 = p1
        }
    }
}