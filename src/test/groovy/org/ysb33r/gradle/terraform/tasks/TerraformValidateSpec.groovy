//
// ============================================================================
// (C) Copyright Schalk W. Cronje 2017
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
// ============================================================================
//

package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import spock.lang.Specification

class TerraformValidateSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    void 'Create TerraformValidate task'() {

        when: 'The terraform plugin is applied'
        project.apply plugin : 'org.ysb33r.terraform.base'

        and: 'A TerraformValidate task is created'
        TerraformPlan task = project.tasks.create('terraformValidate',TerraformPlan)

        then: 'Variables will be checked by default'
        task.checkVariables == true

        when: 'The task is configured'
        project.allprojects {
            terraform {
                executable version : '1.0'
            }

            // tag::configure-terraform-task[]
            terraformValidate {
                checkVariables false // <1>

                variables { // <2>
                    var 'aVar', 'a-value'
                    map 'aMap', foo : 'bar', john : 'doe'
                    list 'aList', 'a-value', 'another-value'
                    list 'bList', ['bee','honey']
                    file 'a.tfvars'
                    file 'b.tfvars'
                }
                source { // <3>
                    dir 'src/cloud'
                }
            }
            // end::configure-terraform-task[]

            // tag::configure-environment[]
            terraformValidate {
                setEnvironment System.getenv() // <1>

                environment foo :  'bar' // <2>
            }
            // end::configure-environment[]
        }

        then: 'The task attributes are set'
        task.checkVariables == false
        task.variables.files.files == [project.file('a.tfvars'),project.file('b.tfvars')] as Set
        task.variables.vars.size() == 4 // No need for detailed test (arlady done in VariablesSpec).
        task.environment['foo'] == 'bar'
        task.source.GetInputSource == project.file('src/cloud')

        when: 'The terraform file location is configured via the task extension'
        project.allprojects {
            // tag::configure-terraform-extension[]
            terraformValidate {
                terraform {
                    executable path : '/path/to/terraform' // <1>
                }
            }
            // end::configure-terraform-extension[]
        }

        then: 'The value in the project extension is no longer used'
        ((TerraformExtension)project.terraform).resolvableExecutable != task.terraform.resolvableExecutable

        when: 'Creating the execution specification'
        TerraformExecSpec execSpec = task.buildExecSpec()
        String dummyTerraform = new File('/path/to/terraform').absolutePath

        then:
        execSpec.commandLine == [
            dummyTerraform,
            'validate',
            '-check-variables=false',
            '-var', 'aVar=a-value',
            '-var', 'aMap={"foo" : "bar", "john" : "doe"}',
            '-var', 'aList=["a-value", "another-value"]',
            '-var', 'bList=["bee", "honey"]',
            '-var-file=' + project.file('a.tfvars').absolutePath,
            '-var-file=' + project.file('b.tfvars').absolutePath,
            project.file('src/cloud').absolutePath
        ]

    }
}