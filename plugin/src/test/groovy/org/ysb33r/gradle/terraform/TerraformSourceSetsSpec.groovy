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
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import spock.lang.Issue
import spock.lang.Specification

class TerraformSourceSetsSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/issues/1')
    void 'Variable definitions in source set should not create new source sets'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'

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
                }
            }
        }

        NamedDomainObjectContainer<TerraformSourceDirectorySet> tss = project.terraformSourceSets
        def variables = ((Variables) tss.getByName('main').variables)

        then:
        verifyAll {
            variables.vars.foo1 == 'bar1'
        }
    }

    @Issue('https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/issues/1')
    void 'test different variables supported'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'

        when:
        Map<String, ?> myMap = [name: 'John', age: 30, city: 'New York']
        project.allprojects {
            terraformSourceSets {
                main {
                    variables {
                        var 'abc', 'abcde'
                        // TODO support
                        //map 'someMap' myMap
                        //list 'list' listOf('bar1', 'bar2')
                    }
                }
            }
        }

        NamedDomainObjectContainer<TerraformSourceDirectorySet> tss = project.terraformSourceSets
        def variables = ((Variables) tss.getByName('main').variables)

        then:
        verifyAll {
            variables.vars.abc == 'abcde'
            //allVars.vars.someMap.name == 'John'
        }
    }

    void 'Items must be able resolve entities in project scope'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'
        project.extensions.create('testExt', TestExtension, project.providers, project)

        when:
        configureFourSourceSets()

        then:
        noExceptionThrown()
    }

    void 'Items must be able resolve entities in project scope even with different order of plugins applied'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'
        project.extensions.create('testExt', TestExtension, project.providers, project)

        when:
        configureFourSourceSets()

        then:
        noExceptionThrown()
    }

    void 'Can provide a tfvars file'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'

        project.allprojects {
            terraformSourceSets {
                main {
                    variables {
                        file( 'foo.tfvars')
                        file( 'foo2.tfvars')
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
        // vars.fileNames.contains('foo.tfvars')
        cmdline.contains("-var-file=${project.file('src/main/tf/foo.tfvars')}".toString())

        and:
        fooPos < foo2Pos
    }

    void 'source sets'() {
        setup:
        project.apply plugin: 'foo.bar.terraform'
        project.allprojects {
            terraformSourceSets {
                main {
                    srcDir = file('foo/bar')
                    backendText("hi")
                    // should support backendFile...
                    // just variables files... these can be json or tfvars
                    variables {
                        file 'filename.tf'
                        file 'foo.tf'
                    }
                }
                release
            }
        }

        expect:
        def tss = project.terraformSourceSets
        def mainSourceSet = tss.getByName('main') as TerraformSourceDirectorySet
        mainSourceSet.srcDir.get() == project.file('foo/bar')
        mainSourceSet.backendPropertyText().get() == "hi"
        //Set<String> files = ["filename.tf", "foo.tf"].toSet()
        //mainSourceSet.variables.fileNames == files

        def releaseSourceSet = tss.getByName('release')
        releaseSourceSet.srcDir.get() == project.file('src/release/tf')
    }

    void configureFourSourceSets() {
        project.allprojects {
            terraformSourceSets {
                main {
                }
                create('created') {
                }
                register('registered') {
                }
                groovyAutoAddStyle {
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