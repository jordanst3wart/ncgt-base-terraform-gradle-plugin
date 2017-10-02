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
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.internal.Downloader
import org.ysb33r.grolifant.api.StringUtils
import org.ysb33r.grolifant.api.exec.AbstractToolExtension
import org.ysb33r.grolifant.api.exec.ResolveExecutableByVersion

import java.util.concurrent.Callable

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
    public static final String NAME = 'terraform'

    /** The default version of Packer that will be used on
     * a supported platform if nothing else is configured.
     */
    public static final String TERRAFORM_DEFAULT = '0.10.6'

    /** Constructs a new extension which is attached to the provided project.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Project project) {
        super(project)
        if (Downloader.downloadSupported) {
            addVersionResolver(project)
            executable([version: TERRAFORM_DEFAULT] as Map<String, Object>)
        } else {
            executable searchPath()
        }
        this.warnOnNewVersion = false
        this.pluginCacheDir = "${project.buildDir}/terraform-providers"
    }

    /** Constructs a new extension which is attached to the provided task.
     *
     * @param project Project this extension is associated with.
     */
    TerraformExtension(Task task) {
        super(task, NAME)
    }

    /** Use this to configure a system path search for {@code Terraform}.
     *
     * @return Returns a special option to be used in {@link #executable}
     */
    static Map<String, Object> searchPath() {
        TerraformExtension.SEARCH_PATH
    }


    /** Print a warning message if a new version of {@code terraform} is available.
     *
     */
    boolean getWarnOnNewVersion() {
        (this.warnOnNewVersion == null && task != null) ? globalExtension.getWarnOnNewVersion() : this.warnOnNewVersion
    }

    /** Turn checkpoint warning on or off
     *
     * @param value {@code true} to warn on new {@code terraform} versions.
     */
    void setWarnOnNewVersion(boolean value) {
        this.warnOnNewVersion = value
    }

    /** Turn checkpoint warning on or off
     *
     * @param value {@code true} to warn on new {@code terraform} versions.
     */
    void warnOnNewVersion(boolean value) {
        this.warnOnNewVersion = value
    }

    File getPluginCacheDir() {
        (this.pluginCacheDir == null && task != null) ? globalExtension.getPluginCacheDir() : getProject().file(this.pluginCacheDir)
    }

    void setPluginCacheDir(Object path) {
        this.pluginCacheDir = path
    }

    void pluginCacheDir(Object path) {
        this.pluginCacheDir = path
    }

    String getWorkspace() {
        (this.workspace == null && task != null) ? globalExtension.getWorkspace() : this.workspace
    }

    void setWorkspace(final String ws) {
        this.workspace = ws
    }

    void workspace(final String ws) {
        this.workspace = ws
    }

    // -------------------------------------------------------------------
    ResolvableExecutableType resolvableExecutableType
    static class ResolvableExecutableType {
        ResolvableExecutableType(String t, Provider<String> v) {
            this.type = t
            this.value = v
        }

        final String type
        final Provider<String> value
    }
    @Override
    void executable(Map<String, ?> opts) {
        if(opts['version']) {
            resolvableExecutableType = new ResolvableExecutableType(
                'version',
                project.providers.provider({
                    StringUtils.stringize(opts['version'])
                } as Callable<String>)
            )
        } else if(opts['search']) {
            resolvableExecutableType = new ResolvableExecutableType(
                'search',
                project.providers.provider({
                    StringUtils.stringize(opts['search'])
                } as Callable<String>)
            )
        } else if(opts['path']) {
            resolvableExecutableType = new ResolvableExecutableType(
                'path',
                project.providers.provider({
                    StringUtils.stringize(opts['path'])
                } as Callable<String>)
            )
        }
        super.executable(opts)
    }
    // -------------------------------------------------------------------

    private TerraformExtension getGlobalExtension() {
        (TerraformExtension) getProjectExtension()
    }

    private Boolean warnOnNewVersion
    private Object pluginCacheDir
    private String workspace

    private void addVersionResolver(Project project) {

        ResolveExecutableByVersion.DownloaderFactory downloaderFactory = {
            Map<String, Object> options, String version, Project p ->
                new Downloader(version, p)
        } as ResolveExecutableByVersion.DownloaderFactory

        ResolveExecutableByVersion.DownloadedExecutable resolver = { Downloader installer ->
            installer.getTerraformExecutablePath()
        } as ResolveExecutableByVersion.DownloadedExecutable

        getResolverFactoryRegistry().registerExecutableKeyActions(
            new ResolveExecutableByVersion(project, downloaderFactory, resolver)
        )
    }


    private static final Map<String, Object> SEARCH_PATH = [search: 'terraform'] as Map<String, Object>

}
