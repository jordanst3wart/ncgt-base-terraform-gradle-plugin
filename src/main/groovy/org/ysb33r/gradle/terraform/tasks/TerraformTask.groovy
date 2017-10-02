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

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.olifant.MapUtils
import org.ysb33r.gradle.olifant.exec.AbstractExecWrapperTask
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension

/** A task for performing a {@code terraform} execution.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformTask extends AbstractExecWrapperTask<TerraformExecSpec,TerraformExtension> {

    TerraformTask() {
        super()
        terraformExtension = (TerraformExtension)(extensions.create(TerraformExtension.NAME,TerraformExtension,this))

        if(project.gradle.startParameter.rerunTasks) {
            force = true
        }
    }

    /** Force the build to be redone.
     *
     * <p> False by default unless Gradle was run with {@code --rerun-tasks} in which case the default is {@b true}.
     */
    @Internal
    boolean force = false

    /** Build images in parallel.
     *
     * <p> True by default
     */
    @Internal
    boolean parallel = true

    /** Images to exclude from template file.
     *
     * @return List of images to exclude. Default is to exclude nothing.
     */
    @Internal
    Iterable<String> getExcludes() {
        this.excludes
    }

    /** Replace current exclude list with a new list.
     *
     * @param args New exclusion list
     */
    void setExcludes(Iterable<String> args) {
        this.excludes.clear()
        this.excludes.addAll(args)
    }

    /** Add additional images to exclude.
     *
     * @param args List of excluded images.
     */
    void excludes(Iterable<String> args) {
        this.excludes.addAll(args)
    }

    /** Add additional images to exclude.
     *
     * @param args List of excluded images.
     */
    void excludes(String... args) {
        excludes(args as List)
    }

    /** Images to include from template file.
     *
     * @return List of images to include. Default is to inlude everything.
     */
    @Internal
    Iterable<String> getIncludes() {
        this.includes
    }

    /** Replace current include list with a new list.
     *
     * @param args New inclusion list
     */
    void setIncludes(Iterable<String> args) {
        this.includes.clear()
        this.includes.addAll(args)
    }

    /** Add additional images to included.
     *
     * @param args List of included images.
     */
    void includes(Iterable<String> args) {
        this.includes.addAll(args)
    }

    /** Add additional images to include.
     *
     * @param args List of included images.
     */
    void includes(String... args) {
        includes(args as List)
    }

    /** Variables to pass to {@code Packer}.
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return List of variables that will be passed.
     */
    @Input
    Map<String,String> getVars() {
        MapUtils.stringizeValues(this.vars)
    }

    /** Replace current variable property list with a new list.
     *
     * @param args New variable key-value set of properties.
     */
    void setVars(Map<String,?> args) {
        this.vars.clear()
        this.vars.putAll((Map<String,Object>)args)
    }

    /** Add variables to be passed to {@code Packer}.
     *
     * @param args Variable key-value set of additional properties.
     */
    void vars(Map<String,?> args) {
        this.vars.putAll((Map<String,Object>)args)
    }

    /** The template file to use for the images.
     *
     * @return Resolved template file
     */
    @InputFile
    File getTemplate() {
        project.file(templateFile)
    }

    /** Set template file to use.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void setTemplate(Object templateFile) {
        this.templateFile = templateFile
    }

    /** Set template file to use.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void template(Object templateFile) {
        this.templateFile = templateFile
    }

    /** The output directory to use for artifacts where applicable for image types.
     *
     * @return Output directory.
     */
    File getOutputDir() {
        project.file(this.outputDir)
    }

    /** Set the output directory to use for artifacts where applicable for image types.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void setOutputDir(Object outDir) {
        this.outputDir = outDir
    }

    /** Set the output directory to use for artifacts where applicable for image types.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void outputDir(Object outDir) {
        this.outputDir = outDir
    }

    @Override
    void exec() {
        getOutputDir().mkdirs()
        super.exec()
    }

    @Override
    protected TerraformExecSpec createExecSpec() {
        new TerraformExecSpec(project,getToolExtension().getResolver())
    }

    @Override
    protected TerraformExecSpec configureExecSpec(TerraformExecSpec execSpec) {
        execSpec.command 'build'

        if(!this.excludes.empty) {
            execSpec.cmdArgs "-except=${this.excludes.join(',')}"
        }

        if(!this.includes.empty) {
            execSpec.cmdArgs "-only=${this.includes.join(',')}"
        }

        if(this.force) {
            execSpec.cmdArgs '-force'
        }

        if(!this.parallel) {
            execSpec.cmdArgs '-parallel=false'
        }

        getVars().each { String key,String val ->
            execSpec.cmdArgs '-var', "${key}=${val}"
        }

        execSpec.environment getEnvironment()
        execSpec.workingDir getOutputDir()
        execSpec.cmdArgs template.absolutePath

        return execSpec
    }


    @Override
    protected TerraformExtension getToolExtension() {
        this.terraformExtension
    }

    // Internal method used for testing
    @PackageScope TerraformExecSpec buildExecSpec() {
        TerraformExecSpec execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        configureExecSpec(execSpec)
    }


    private Object templateFile
    private Object outputDir = "${project.buildDir}/packer"
    private final TerraformExtension terraformExtension
    private final List<String> includes = []
    private final List<String> excludes = []
    private final Map<String,Object> vars = [:]
    private final Map<String,Object> env = [:]
}
