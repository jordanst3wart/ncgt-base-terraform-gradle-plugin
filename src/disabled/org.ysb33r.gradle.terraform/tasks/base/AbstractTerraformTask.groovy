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

package org.ysb33r.gradle.terraform.tasks.base

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.configuration.ConsoleOutput
import org.ysb33r.grolifant.api.errors.ExecutionException
import org.ysb33r.grolifant.api.exec.AbstractExecWrapperTask
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import Lock
import Source
import State
import StateOut

/** A base class for performing a {@code terraform} execution.
 *
 * @since 0.1
 */
@CompileStatic
abstract class AbstractTerraformTask extends AbstractExecWrapperTask<TerraformExecSpec,TerraformExtension> {

    protected AbstractTerraformTask() {
        super()
        terraformExtension = (TerraformExtension)(extensions.create(TerraformExtension.NAME,TerraformExtension,this))

        if(project.gradle.startParameter.isOffline()) {
            environment 'TF_SKIP_REMOTE_TESTS' : '1'
        }
//        if(project.gradle.startParameter.rerunTasks) {
//            force = true
//        }

    }
//    /** Force the build to be redone.
//     *
//     * <p> False by default unless Gradle was run with {@code --rerun-tasks} in which case the default is {@b true}.
//     */
//    @Internal
//    boolean force = false
//
//    /** Build images in parallel.
//     *
//     * <p> True by default
//     */
//    @Internal
//    boolean parallel = true
//
//    /** Images to exclude from template file.
//     *
//     * @return List of images to exclude. Default is to exclude nothing.
//     */
//    @Internal
//    Iterable<String> getExcludes() {
//        this.excludes
//    }
//
//    /** Replace current exclude list with a new list.
//     *
//     * @param args New exclusion list
//     */
//    void setExcludes(Iterable<String> args) {
//        this.excludes.clear()
//        this.excludes.addAll(args)
//    }
//
//    /** Add additional images to exclude.
//     *
//     * @param args List of excluded images.
//     */
//    void excludes(Iterable<String> args) {
//        this.excludes.addAll(args)
//    }
//
//    /** Add additional images to exclude.
//     *
//     * @param args List of excluded images.
//     */
//    void excludes(String... args) {
//        excludes(args as List)
//    }
//
//    /** Images to include from template file.
//     *
//     * @return List of images to include. Default is to inlude everything.
//     */
//    @Internal
//    Iterable<String> getIncludes() {
//        this.includes
//    }
//
//    /** Replace current include list with a new list.
//     *
//     * @param args New inclusion list
//     */
//    void setIncludes(Iterable<String> args) {
//        this.includes.clear()
//        this.includes.addAll(args)
//    }
//
//    /** Add additional images to included.
//     *
//     * @param args List of included images.
//     */
//    void includes(Iterable<String> args) {
//        this.includes.addAll(args)
//    }
//
//    /** Add additional images to include.
//     *
//     * @param args List of included images.
//     */
//    void includes(String... args) {
//        includes(args as List)
//    }
//
//    /** Variables to pass to {@code Packer}.
//     *
//     * <p> Calling this will resolve all lazy-values in the variable map.
//     *
//     * @return List of variables that will be passed.
//     */
//    @Input
//    Map<String,String> getVars() {
//        MapUtils.stringizeValues(this.vars)
//    }
//
//    /** Replace current variable property list with a new list.
//     *
//     * @param args New variable key-value set of properties.
//     */
//    void setVars(Map<String,?> args) {
//        this.vars.clear()
//        this.vars.putAll((Map<String,Object>)args)
//    }
//
//    /** Add variables to be passed to {@code Packer}.
//     *
//     * @param args Variable key-value set of additional properties.
//     */
//    void vars(Map<String,?> args) {
//        this.vars.putAll((Map<String,Object>)args)
//    }
//
//    /** The template file to use for the images.
//     *
//     * @return Resolved template file
//     */
//    @InputFile
//    File getTemplate() {
//        project.file(templateFile)
//    }
//
//    /** Set template file to use.
//     *
//     * @param template Anything that can be resolved by {@code project.file}
//     */
//    void setTemplate(Object templateFile) {
//        this.templateFile = templateFile
//    }
//
//    /** Set template file to use.
//     *
//     * @param template Anything that can be resolved by {@code project.file}
//     */
//    void template(Object templateFile) {
//        this.templateFile = templateFile
//    }
//
//    /** The output directory to use for artifacts where applicable for image types.
//     *
//     * @return Output directory.
//     */
//    File getOutputDir() {
//        project.file(this.outputDir)
//    }
//
//    /** Set the output directory to use for artifacts where applicable for image types.
//     *
//     * @param template Anything that can be resolved by {@code project.file}
//     */
//    void setOutputDir(Object outDir) {
//        this.outputDir = outDir
//    }
//
//    /** Set the output directory to use for artifacts where applicable for image types.
//     *
//     * @param template Anything that can be resolved by {@code project.file}
//     */
//    void outputDir(Object outDir) {
//        this.outputDir = outDir
//    }

        /** The output directory to use for artifacts where applicable for image types.
     *
     * @return Output directory.
     */
    File getSourceDir() {
        project.file(this.sourceDir)
    }

    /** Set the output directory to use for artifacts where applicable for image types.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void setSourceDir(Object outDir) {
        this.sourceDir = outDir
    }

    /** Set the output directory to use for artifacts where applicable for image types.
     *
     * @param template Anything that can be resolved by {@code project.file}
     */
    void sourceDir(Object outDir) {
        this.sourceDir = outDir
    }

    @Override
    void exec() {
        File pluginCacheDir = getToolExtension().getPluginCacheDir().absoluteFile
        if(!pluginCacheDir.exists()) {
            pluginCacheDir.mkdirs()
        }
        super.exec()
    }

    @Override
    protected TerraformExecSpec createExecSpec() {
        new TerraformExecSpec(project,getToolExtension().getResolver())
    }

    @Override
    protected TerraformExecSpec configureExecSpec(TerraformExecSpec execSpec) {
        execSpec.command getTerraformCommand()

        if(!getToolExtension().warnOnNewVersion) {
            execSpec.environment CHECKPOINT_DISABLE : '1'
        }

        execSpec.environment TF_PLUGIN_CACHE_DIR : getToolExtension().getPluginCacheDir().absolutePath

        final String workspace = getToolExtension().workspace
        if(workspace) {
            execSpec.environment TF_WORKSPACE : workspace
        }

//
//        if(!this.excludes.empty) {
//            execSpec.cmdArgs "-except=${this.excludes.join(',')}"
//        }
//
//        if(!this.includes.empty) {
//            execSpec.cmdArgs "-only=${this.includes.join(',')}"
//        }
//
//        if(this.force) {
//            execSpec.cmdArgs '-force'
//        }
//
//        if(!this.parallel) {
//            execSpec.cmdArgs '-parallel=false'
//        }
//
//        getVars().each { String key,String val ->
//            execSpec.cmdArgs '-var', "${key}=${val}"
//        }
//
//        execSpec.environment getEnvironment()
//        execSpec.workingDir getOutputDir()
//        execSpec.cmdArgs template.absolutePath


        addCommandSpecificsToExecSpec(execSpec)
    }

    /** Set the command to be executed by {@code terraform}.
     *
     * @param command Command to be executed. See https://www.terraform.io/docs/commands/index.html for details.
     */
    protected void setTerraformCommand(final String command) {
        this.command = command
    }

    @Override
    protected TerraformExtension getToolExtension() {
        this.terraformExtension
    }

    /** To be called from tasks where the command supports {@code no-color}.
     *
     * <p> Will get set if {@code --console=plain was provided to Gradle}
     *
     * @param execSpec ExecSpec to configure
     */
    protected void color(TerraformExecSpec execSpec) {
        ConsoleOutput mode = project.gradle.startParameter.consoleOutput
        if(mode == ConsoleOutput.Plain || mode == ConsoleOutput.Auto && System.getenv('TERM') == 'dumb') {
            execSpec.cmdArgs '-no-color'
        }
    }

    /** To be calle dfomr takss where the command supports {@code input}
     *
     * @param execSpec ExecSpec to configure
     */
    protected void noInput(TerraformExecSpec execSpec) {
        execSpec.cmdArgs '-input=false'
    }

    /** Adds a boolean command-line option with correct formatting to the execution specification.
     *
     * @param execSpec ExecSpec to configure
     * @param optionName Name of option
     * @param value Boolean value
     */
    protected void addBooleanCmdLineOption( TerraformExecSpec execSpec, final String optionName, boolean value) {
        execSpec.cmdArgs "-${optionName}=${value?'true':'false'}"
    }

    protected void addVariablesToCmdLine( TerraformExecSpec execSpec, final Map<String,String> vars ) {
        vars.each { String key,String val ->
            execSpec.cmdArgs '-var', "${key}=${val}"
        }
    }

    protected void addVariableFilesToCmdLine( TerraformExecSpec execSpec, final FileCollection collection ) {
        for( File f in collection.files ) {
            execSpec.cmdArgs "-var-file=${f.absolutePath}"
        }
    }

    protected void addSourceDirToCmdLine( TerraformExecSpec execSpec, final Source src) {
        execSpec.cmdArgs src.getInputSource().absolutePath
    }

    protected void addLockOptionsToCmdLine( TerraformExecSpec execSpec, final Lock lock) {
        addBooleanCmdLineOption(execSpec,'lock',lock.enabled)
        execSpec.cmdArgs "-lock-timeout=${lock.timeout}"
    }

    protected void addStateOptionsToCmdLine( TerraformExecSpec execSpec, final State state) {
        File path = state.getPath()
        if(path != null) {
            execSpec.cmdArgs "-state=${path.absolutePath}"
        }

        if(state instanceof StateOut) {
            path = ((StateOut)state).getStateOut()
            if(path != null) {
                execSpec.cmdArgs "-state-out=${path.absolutePath}"
            }
        }
    }

    protected void addTargetsToCmdLine( TerraformExecSpec execSpec, final Iterable<String> targets) {
        execSpec.cmdArgs (targets.collect { String t -> "-target=${t}".toString() } )
    }

    protected void addMaxParallel( TerraformExecSpec execSpec, int maxParallel) {
        if(maxParallel>0) {
            execSpec.cmdArgs "-parallelism=${maxParallel}"
        }
    }

    protected void addModuleDepthToCmdLine( TerraformExecSpec execSpec, int moduleDepth) {
        execSpec.cmdArgs "-module-depth=${moduleDepth}"
    }

    protected void configureNested( Object nested, Closure cfg ) {
        Closure c = (Closure)(cfg.clone())
        c.delegate = nested
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
    }

    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    abstract protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec)


    // Internal method used for testing
    @PackageScope TerraformExecSpec buildExecSpec() {
        TerraformExecSpec execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        configureExecSpec(execSpec)
    }

    private String getTerraformCommand() {
        if(this.command == null) {
            throw new ExecutionException("Terraform command was not set")
        }
        this.command
    }

    private String command
    private Object sourceDir = "${project.projectDir}/src/terraform"
    private final TerraformExtension terraformExtension
}
