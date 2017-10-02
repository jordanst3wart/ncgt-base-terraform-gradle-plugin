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
import spock.lang.PendingFeature
import spock.lang.Specification

class TerraformApplyChangesSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    @PendingFeature
    void 'Create Terraform task'() {

        when: 'The terraform plugin is applied'
        project.apply plugin : 'org.ysb33r.terraform.base'

        then: 'A TerraformBuilder task is created'
        project.tasks.getByName('terraformBuild')

        when: 'The task is inspected'
        TerraformApplyChanges task = project.tasks.getByName('terraformBuild')

        then: 'A terraform extensions is added to the task'
        task.extensions.getByName('terraform') instanceof TerraformExtension

        and:
        task.force == false
        task.parallel == true
        task.excludes.empty
        task.includes.empty

        when: 'Configuring the task'
        project.allprojects {
            // tag::configure-terraform-task[]
            terraformBuild {
                includes = [ 'box1' ]   // <1>
                includes 'box2', 'box3' // <2>
                excludes = [ 'box4' ]   // <3>
                excludes 'box5', 'box6' // <4>

                force true              // <5>
                parallel false          // <6>

                template  '/path/to/spec.json' // <7>
                outputDir "${buildDir}/work"   // <8>

                vars aws_access_key : 'MY_ACCESS_KEY',
                    aws_secret_key : { 'MY SECRET KEY' } // <9>
            }
            // end::configure-terraform-task[]

            // tag::configure-environment[]
            terraformBuild {
                setEnvironment System.getenv() // <1>

                environment foo :  'bar' // <2>
            }
            // end::configure-environment[]
        }

        then: 'The task attributes are set'
        task.includes.containsAll(['box1','box2','box3'])
        task.excludes.containsAll(['box4','box5','box6'])
        task.force == true
        task.parallel == false
        task.template == project.file('/path/to/spec.json')
        task.outputDir == project.file("${project.buildDir}/work")
        task.vars['aws_access_key'] == 'MY_ACCESS_KEY'
        task.vars['aws_secret_key'] == 'MY SECRET KEY'
        task.environment['foo'] == 'bar'

        when: 'The terraform file location is configured via the task extension'
        project.allprojects {
            // tag::configure-terraform-extension[]
            terraformBuild {
                terraform {
                    executable version : '1.0.0' // <1>
                }
            }
            // end::configure-terraform-extension[]
        }

        then: 'The value in the project extension is no longer used'
        ((TerraformExtension)project.terraform).resolvedTerraformExecutable != task.terraform.resolvedTerraformExecutable
    }

    @PendingFeature
    void 'Variables must be passed correctly'() {
        setup:
        project.apply plugin : 'org.ysb33r.terraform.base'
        String dummyTerraform = new File('/path/to/terraform').absolutePath

        project.allprojects {
            terraform {
                executable path : dummyTerraform
            }
        }
        TerraformApplyChanges task = project.tasks.create('builder',TerraformApplyChanges)
        task.outputDir = new File('foo').absoluteFile

        task.vars foo : 'ba r'
        task.template 'foo.json'

        when:
        TerraformExecSpec execSpec = task.buildExecSpec()

        then:
        execSpec.commandLine == [
            dummyTerraform,
            'build',
            '-var',
            "foo=ba r",
            new File(project.projectDir,'foo.json').absolutePath
        ]

    }
}