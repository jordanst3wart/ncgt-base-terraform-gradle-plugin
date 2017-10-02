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

package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.ysb33r.gradle.olifant.exec.AbstractToolExtension
import org.ysb33r.gradle.olifant.exec.ResolvableExecutable
import org.ysb33r.gradle.olifant.exec.ResolveExecutableByVersion
import org.ysb33r.gradle.terraform.internal.Downloader

/** Configure project defaults or task specifics for {@code Terraform}.
 *
 * This also allows the {@code terraform} executable to be set
 *
 * It can be passed by a single map option.
 *
 * <code>
 *   // By tag (Gradle will download and cache the correct distribution).
 *   executable tag : '0.10.1'
 *
 *   // By a physical path (
 *   executable path : '/path/to/terraform'
 *
 *   // By using searchPath (will attempt to locate in search path).
 *   executable searchPath()
 * </code>
 *
 * If this build runs on a platform that supports downloading of the {@code terraform} executable
 * the default will be to use the version as specified by {@link TerraformExtension#TERRAFORM_DEFAULT},
 * otherwise it will be in search mode.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformExtension extends AbstractToolExtension {

    /** The standard extension name.
     *
     */
    static final String NAME = 'terraform'

    /** The default version of Packer that will be used on
     * a supported platform if nothing else is configured.
     */
    static final String TERRAFORM_DEFAULT = '0.10.6'

    /** Constructs a new extension which is attached to the provided project.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Project project) {
        super(project)
        if(Downloader.downloadSupported) {
            addVersionResolver(project)
            executable([ version : TERRAFORM_DEFAULT ] as Map<String,Object>)
        } else {
            executable searchPath()
        }
    }

    /** Constructs a new extension which is attached to the provided task.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Task task) {
        super(task,NAME)
    }

    /** Use this to configure a system path search for {@code Terraform}.
     *
     * @return Returns a special option to be used in {@link #executable}
     */
    static Map<String,Object> searchPath() {
        TerraformExtension.SEARCH_PATH
    }

    private void addVersionResolver(Project project) {

        ResolveExecutableByVersion.DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version,Project p ->
                new Downloader(version,p)
        } as ResolveExecutableByVersion.DownloaderFactory

        ResolveExecutableByVersion.DownloadedExecutable resolver = { Downloader installer ->
            installer.getTerraformExecutablePath()
        } as ResolveExecutableByVersion.DownloadedExecutable

        getResolverFactoryRegistry().registerExecutableKeyActions(
            new ResolveExecutableByVersion(project,downloaderFactory,resolver)
        )
    }


    private static final Map<String,Object> SEARCH_PATH = [ search : 'terraform' ] as Map<String,Object>

}
