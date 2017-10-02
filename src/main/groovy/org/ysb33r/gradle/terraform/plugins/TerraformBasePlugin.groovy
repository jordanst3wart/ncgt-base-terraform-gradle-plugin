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

package org.ysb33r.gradle.terraform.plugins

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.ysb33r.gradle.terraform.TerraformExtension

/** Provide the basic capabilities for dealing with Terraform tasks. Allow for downloading & caching of Terraform distributions
 * on a variety of the most common development platforms.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformBasePlugin implements Plugin<Project> {
    static final String DEPENDENCY_EXTENSION = 'terraformProvider'
    static final String TERRAFORM_CONFIGURATION = 'terraform'

    void apply(Project project) {
        project.extensions.create(TerraformExtension.NAME, TerraformExtension, project)

//        project.extensions.extraProperties.set(DEPENDENCY_EXTENSION, { final String providerName, final String ver = '+' ->
//            DependencyFactory.newSelfResolvingDependency(TerraformProviderDependency,project,providerName,ver)
//        })

        project.configurations.create(TERRAFORM_CONFIGURATION) { Configuration cfg ->
            cfg.setVisible(false)
            cfg.setTransitive(false)
            cfg.setCanBeConsumed(false)
        }
    }

}
