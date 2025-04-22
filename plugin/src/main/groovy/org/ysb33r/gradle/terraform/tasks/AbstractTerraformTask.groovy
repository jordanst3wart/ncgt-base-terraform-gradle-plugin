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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Transformer
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.grolifant.api.core.ProjectOperations

/** A base class for performing a {@code terraform} execution.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.1
 */
@CompileStatic
@SuppressWarnings('MethodCount')
class AbstractTerraformTask extends DefaultTask {

    void setSourceSet(TerraformSourceDirectorySet sourceSet) {
        this.sourceSet = project.providers.provider { sourceSet }
    }

    @Internal
    Provider<TerraformSourceDirectorySet> getSourceSet() {
        this.sourceSet
    }

    /** The level at which Terraform should log to a file.
     *
     * @return Terraform log level. Can be {@code null} signifying that logging is switched off.
     */
    @Internal
    String getLogLevel() {
        this.terraformLogLevel
    }

    /**
     * Whether to log progress to the directory specified in.
     *
     * @param state {@code true} to log progress
     *
     * @since 0.10.0
     */
    void setLogProgress(boolean state) {
        this.terraformLogLevel = state ? 'TRACE' : null
    }

    @TaskAction
    void exec() {
        if (terraformLogLevel) {
            sourceSet.get().logDir.get().mkdirs()
        }

        TerraformUtils.terraformLogFile(name, sourceSet.get().logDir).delete()
        TerraformExecSpec execSpec = buildExecSpec()
        Action<ExecSpec> runner = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }
        logger.info("Using Terraform environment: ${terraformEnvironment}")
        logger.debug("Terraform executable will be launched with environment: ${execSpec.environment}")
        // TODO logger.lifecycle...???
        // TODO remove stdoutCapture
        if (this.stdoutCapture) {
            // runs for just show task
            this.stdoutCapture.get().withOutputStream { strm ->
                execSpec.standardOutput(strm)
                project.exec(runner).assertNormalExitValue()
            }
        } else {
            // runs for everything else
            project.exec(runner).assertNormalExitValue()
        }
    }

    /** Command-line parameter for no colour.
     *
     */
    protected static final String NO_COLOR = '-no-color'

    /** Command-line parameter for JSON output.
     *
     */
    protected static final String JSON_FORMAT = '-json'

    /**
     *
     * @param command Command to be executed. See https://www.terraform.io/docs/commands/index.html for details.
     * @param configExtensions Configuration extensions to be added to this task.
     * @param terraformConfigExtensions Configuration extensions that are added to the terraform task extension.
     */
    protected AbstractTerraformTask(
        String cmd,
        List<Class> configExtensions
    ) {
        this.projectOperations = ProjectOperations.find(project)
        this.projectTerraform = project.extensions.getByType(TerraformExtension)
        this.terraformrc = TerraformConfigUtils.locateTerraformRCExtension(project)
        this.command = cmd
        // not defined at setup time
        this.sourceSet = project.provider { null } as Provider<TerraformSourceDirectorySet>
        withConfigExtensions(configExtensions)
    }

    /**
     * Obtain a list of associated variables, should if be a valid condition for the task.
     *
     * In most cases this will be empty.
     *
     * @return Associated variables in terraform format.
     */
    @Internal
    List<Provider<List<String>>> getTfVarProviders() {
        this.tfVarProviders
    }

    @Internal
    protected List<Provider<List<String>>> getCommandLineProviders() {
        this.commandLineProviders
    }

    @Internal
    protected List<String> getDefaultCommandParameters() {
        this.defaultCommandParameters
    }

    @Internal
    protected Provider<File> getStdoutCapture() {
        this.stdoutCapture
    }

    /** Project operations that replaces legacy methods on the {@link org.gradle.api.Project} class and which
     * are safe to use in configuration cache environments.
     */
    @Internal
    protected ProjectOperations getProjectOperations() {
        this.projectOperations
    }

    protected Provider<List<String>> sourceSetVariables() {
        project.provider {
            def variables = this.sourceSet.get().variables
            def configExtension = variables as TerraformTaskConfigExtension
            // TODO this shouldn't work
            configExtension.commandLineArgs
        }
    }

    /**
     * Provider to another Terraform task.
     *
     * @param command Terraform command.
     * @return Task provider.
     *
     * @since 0.10
     */
    @CompileDynamic
    protected Provider<AbstractTerraformTask> taskProvider(String command) {
        Provider<String> taskName = projectOperations.provider { ->
            TerraformConvention.taskName(sourceSet.get().name, command)
        }

        taskName.flatMap({ String it ->
            project.tasks.named(it, AbstractTerraformTask)
        } as Transformer<Provider<AbstractTerraformTask>, String>)
    }

    /**
     * Marks task to always be out of date.
     *
     * Calls this from the constructor of Terraform task types that should always be out of date.
     *
     * @since 0.10.
     */
    protected void alwaysOutOfDate() {
        inputs.property('always-out-of-date', UUID.randomUUID().toString())
    }

    /**
     * To be called from tasks where the command supports {@code input}.
     */
    protected void supportsInputs() {
        defaultCommandParameters.add '-input=false'
    }

    /**
     * To be called from tasks where the command supports {@code auto-approve}.
     */
    protected void supportsAutoApprove() {
        defaultCommandParameters.add '-auto-approve'
    }

    /** To be called from tasks where the command supports {@code no-color}.
     *
     * <p> Will get set if {@code --console=plain was provided to Gradle}
     *
     * @param withColor If set to {@code false}, the task will always run without color output.
     */
    protected void supportsColor(boolean withColor = true) {
        ConsoleOutput mode = projectOperations.consoleOutput
        if (mode == ConsoleOutput.Plain ||
            mode == ConsoleOutput.Auto && System.getenv('TERM') == 'dumb' ||
            !withColor
        ) {
            defaultCommandParameters.add NO_COLOR
        }
    }

    @Input
    protected Map<String, String> getTerraformEnvironment() {
        TerraformUtils.terraformEnvironment(
            terraformrc,
            name,
            sourceSet.get().dataDir,
            sourceSet.get().logDir,
            terraformLogLevel
        )
    }

    /** Adds a command-line provider.
     *
     * @param provider
     */
    protected void addCommandLineProvider(Provider<List<String>> provider) {
        this.commandLineProviders.add(provider)
    }

    /**
     * Additional sources that are not in the source set directory, but for which changes will
     * require a re-run of the task.
     *
     * This includes files such as local modules or files that provide variables directly or
     * indirectly.
     *
     * @return List of input files.
     *
     * @since 0.10
     */
    /*@InputFiles
    protected Provider<List<File>> getSecondarySources() {
        this.secondarySources
    }*/

    protected TerraformExecSpec buildExecSpec() {
        TerraformExecSpec execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        configureExecSpec(execSpec)
    }

    protected TerraformExecSpec addExecutableToExecSpec(final TerraformExecSpec execSpec) {
        execSpec.executable(toolExtension.resolvableExecutable.executable.absolutePath)
        execSpec
    }

    @Internal
    protected TerraformExtension getToolExtension() {
        projectTerraform
    }

    /** Configures a {@link TerraformExecSpec}.
     *
     * @param execSpec Specification to be configured
     * @return Configured specification
     */
    protected TerraformExecSpec configureExecSpec(TerraformExecSpec execSpec) {
        configureExecSpecForCmd(execSpec, terraformCommand, defaultCommandParameters)
        addCommandSpecificsToExecSpec(execSpec)
        execSpec
    }

    /** Configures execution specification for a specific command.
     *
     * @param execSpec Specification to configure.
     * @param tfcmd Terraform command.
     * @param cmdParams Default command parameters.
     * @return Configures specification.
     */
    protected TerraformExecSpec configureExecSpecForCmd(
        TerraformExecSpec execSpec,
        String tfcmd,
        List<String> cmdParams
    ) {
        Map<String, String> tfEnv = terraformEnvironment
        execSpec.identity {
            command tfcmd
            workingDir sourceSet.get().srcDir
            environment tfEnv
            cmdArgs cmdParams
        }
        execSpec
    }

    /** Creates a {@link TerraformExecSpec}.
     *
     * @return {@link TerraformExecSpec}. Never {@code null}.
     */
    protected TerraformExecSpec createExecSpec() {
        new TerraformExecSpec(projectOperations, projectTerraform.resolver)
    }

    /** To be called subclass constructor for defining specific configuration extensions that are
     * supported.
     *
     * @param configExtensions
     */
    private void withConfigExtensions(List<Class> configExtensions) {
        for (it in configExtensions) {
            TerraformTaskConfigExtension cex = (TerraformTaskConfigExtension) project.objects.newInstance(it)
            extensions.add(cex.name, cex)
            commandLineProviders.add(projectOperations.provider { -> cex.commandLineArgs })
        }
    }

    /** When command is run, capture the standard output
     *
     * @param output Output file
     */
    protected void captureStdOutTo(Provider<File> output) {
        this.stdoutCapture = output
    }

    /** Returns the {@code terraform} command this task is implementing.
     *
     * @return Terraform command as string
     */
    @Internal
    protected String getTerraformCommand() {
        this.command
    }

    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        execSpec.cmdArgs(commandLineProviders*.get().flatten())
        execSpec
    }

    @Internal
    protected TerraformRCExtension getTerraformrc() {
        terraformrc
    }

    private Provider<TerraformSourceDirectorySet> sourceSet
    private String terraformLogLevel = 'TRACE'
    private final String command
    private final ProjectOperations projectOperations
    private final TerraformExtension projectTerraform
    private final TerraformRCExtension terraformrc
    private final List<Provider<List<String>>> commandLineProviders = []
    private final List<Provider<List<String>>> tfVarProviders = []
    private final List<String> defaultCommandParameters = []
    private Provider<File> stdoutCapture
}
