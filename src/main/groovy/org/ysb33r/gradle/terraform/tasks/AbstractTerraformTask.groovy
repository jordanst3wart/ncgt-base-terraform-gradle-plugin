/*
 * Copyright 2017-2020 the original author or authors.
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
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.StringUtils
import org.ysb33r.grolifant.api.v4.exec.AbstractExecWrapperTask

import static org.ysb33r.gradle.terraform.internal.Downloader.OS
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.createPluginCacheDir
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.awsEnvironment
import static org.ysb33r.grolifant.api.core.LegacyLevel.PRE_5_0
import static org.ysb33r.grolifant.api.v4.StringUtils.stringize

/** A base class for performing a {@code terraform} execution.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.1
 */
@CompileStatic
@SuppressWarnings('MethodCount')
abstract class AbstractTerraformTask extends AbstractExecWrapperTask<TerraformExecSpec, TerraformExtension> {

    /**
     *
     * @param source Source set of anything that can be resolved using {@link StringUtils#stringize(Object s)}
     * and looked up as a Terraform source set.
     */
    void setSourceSet(Object source) {
        this.sourceSetProxy = source
    }

    @Internal
    TerraformSourceDirectorySet getSourceSet() {
        switch (this.sourceSetProxy) {
            case null:
                return null
            case TerraformSourceDirectorySet:
                return (TerraformSourceDirectorySet) this.sourceSetProxy
            default:
                project.extensions.getByType(TerraformSourceSets).getByName(stringize(this.sourceSetProxy))
        }
    }

    @InputDirectory
    Provider<File> getSourceDir() {
        this.sourceDirProvider
    }

    @Internal
    Provider<File> getDataDir() {
        this.dataDirProvider
    }

    @Internal
    Provider<File> getLogDir() {
        this.logDirProvider
    }

    @Internal
    Provider<File> getReportsDir() {
        this.reportsDirProvider
    }

    /** The level at which Terraform should log.
     *
     * @return Terraform log level. Can be {@code null} signifying that logging is switched off.
     */
    @Internal
    String getLogLevel() {
        switch (this.terraformLogLevel ?: projectOperations.gradleLogLevel) {
            case LogLevel.DEBUG:
            case LogLevel.INFO:
                return 'TRACE'
            default:
                null
        }
    }

    void unsetLogLevel() {
        this.terraformLogLevel = null
    }

    void setLogLevel(String lvl) {
        this.terraformLogLevel = LogLevel.valueOf(lvl)
    }

    void setLogLevel(LogLevel lvl) {
        this.terraformLogLevel = lvl
    }

    /** Replace current environment with new one.
     *
     * Calling this will also remove any project extension environment from this task.
     *
     * @param args New environment key-value map of properties.
     */
    @Override
    void setEnvironment(Map<String, ?> args) {
        noProjectEnvironment = true
        super.setEnvironment(defaultEnvironment)
        environment(args)
    }

    /** Environment for running the exe
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    @Override
    Map<String, String> getEnvironment() {
        if (noProjectEnvironment) {
            super.environment
        } else {
            Map<String, String> combinedEnv = [:]
            combinedEnv.putAll(projectTerraform.environment)
            combinedEnv.putAll(super.environment)
            combinedEnv
        }
    }

    /** Adds AWS environmental variables to Terraform runtime environment.
     *
     * @since 0.6.0
     */
    void useAwsEnvironment() {
        environment awsEnvironment()
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    String terraformPath(Object file) {
        TerraformUtils.terraformPath(projectOperations, file)
    }

    @Override
    void exec() {
        TerraformExecSpec execSpec = buildExecSpec()
        createPluginCacheDir(terraformrc)

        Action<ExecSpec> runner = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        }

        if (logLevel) {
            logDir.get().mkdirs()
        }

        logger.info "Using Terraform environment: ${terraformEnvironment}"
        if (this.stdoutCapture) {
            this.stdoutCapture.get().withOutputStream { strm ->
                execSpec.standardOutput(strm)
                projectOperations.exec(runner).assertNormalExitValue()
            }
        } else {
            projectOperations.exec(runner).assertNormalExitValue()
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

    /** Project operations that replaces legacy methods on the {@link org.gradle.api.Project} class and which
     * are safe to use in configuration cache environments.
     */
    @Internal
    protected final ProjectOperations projectOperations

    /**
     *
     * @param command Command to be executed. See https://www.terraform.io/docs/commands/index.html for details.
     * @param configExtensions Configuration extensions to be added to this task.
     * @param terraformConfigExtensions Configuration extensions that are added to the terraform task extension.
     */
    protected AbstractTerraformTask(
        String cmd,
        List<Class> configExtensions,
        List<TerraformExtensionConfigTypes> terraformConfigExtensions
    ) {
        super()
        this.projectOperations = ProjectOperations.find(project)
        this.command = cmd
        this.projectTerraform = project.extensions.getByType(TerraformExtension)
        this.terraformrc = TerraformConfigUtils.locateTerraformRCExtension(project)

        terraformExtension = extensions.create(
            TerraformExtension.NAME,
            TerraformExtension,
            this,
            terraformConfigExtensions
        )

        withConfigExtensions(configExtensions)
        withTerraformConfigExtensions(terraformConfigExtensions)
        environment(defaultEnvironment)

        sourceDirProvider = project.provider {
            sourceSet.srcDir.get()
        }

        dataDirProvider = project.provider {
            sourceSet.dataDir.get()
        }

        logDirProvider = project.provider {
            sourceSet.logDir.get()
        }

        reportsDirProvider = project.provider {
            sourceSet.reportsDir.get()
        }

        secondarySources = project.provider { ->
            sourceSet.secondarySources.get()
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
            TerraformConvention.taskName(sourceSet.name, command)
        }

        if (PRE_5_0) {
            taskName.map({ String it ->
                (AbstractTerraformTask) project.tasks.getByName(it)
            } as Transformer<AbstractTerraformTask, String>)
        } else {
            taskName.flatMap({ String it ->
                project.tasks.named(it, AbstractTerraformTask)
            } as Transformer<Provider<AbstractTerraformTask>, String>)
        }
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

    /**
     * To be called from tasks where the command supports {@code yes}.
     *
     * @since 0.10.0
     */
    protected void supportsYes() {
        defaultCommandParameters.add '-yes'
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

    protected void supportsForce() {
        if (projectOperations.rerunTasks) {
            defaultCommandParameters.add '-force'
        }
    }

    /** When command is run, capture the standard output
     *
     * @param output Output file
     */
    protected void captureStdOutTo(Provider<File> output) {
        this.stdoutCapture = output
    }

    protected TerraformExecSpec buildExecSpec() {
        TerraformExecSpec execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        configureExecSpec(execSpec)
    }

    /** Creates a {@link TerraformExecSpec}.
     *
     * @return {@link TerraformExecSpec}. Never {@code null}.
     */
    @Override
    protected TerraformExecSpec createExecSpec() {
        new TerraformExecSpec(projectOperations, toolExtension.resolver)
    }

    /** Configures a {@link TerraformExecSpec}.
     *
     * @param execSpec Specification to be configured
     * @return Configured specification
     */
    @Override
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
            workingDir sourceDir
            environment tfEnv
            cmdArgs cmdParams
        }

        execSpec.environment(environment)
        execSpec
    }

    @SuppressWarnings('DuplicateStringLiteral')
    @Input
    protected Map<String, String> getTerraformEnvironment() {
        TerraformUtils.terraformEnvironment(
            terraformrc,
            name,
            dataDir,
            logDir,
            logLevel
        )
    }

    /**
     * Additional sources that are not in the source set directory, but for which cahnges will
     * require a re-run of the task.
     *
     * This includes files such as local modules or files that provide variables directly or
     * indirectly.
     *
     * @return List of input files.
     *
     * @since 0.10
     */
    @InputFiles
    protected final Provider<List<File>> secondarySources

    /** Adds a boolean command-line option with correct formatting to the execution specification.
     *
     * @param execSpec ExecSpec to configure
     * @param optionName Name of option
     * @param value Boolean value
     */
    protected void addBooleanCmdLineOption(TerraformExecSpec execSpec, final String optionName, boolean value) {
        execSpec.cmdArgs "-${optionName}=${value ? 'true' : 'false'}"
    }

    /** Adds Terraform variables to execution specification.
     *
     * @param execSpec Specification to be configured.
     * @param vars Map of variables to be added.
     */
    protected void addVariablesToCmdLine(TerraformExecSpec execSpec, final Map<String, String> vars) {
        vars.each { String key, String val ->
            execSpec.cmdArgs '-var', "${key}=${val}"
        }
    }

    /** Add files containing variables to command-line.
     *
     * @param execSpec Specification to be configured
     * @param collection Collection of files containing variables.
     */
    protected void addVariableFilesToCmdLine(TerraformExecSpec execSpec, final FileCollection collection) {
        for (File f in collection.files) {
            execSpec.cmdArgs "-var-file=${f.absolutePath}"
        }
    }

    @Override
    @Internal
    protected TerraformExtension getToolExtension() {
        this.terraformExtension
    }

    /** Add specific command-line options for the command.
     /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        execSpec.cmdArgs(commandLineProviders*.get().flatten())
        execSpec
    }

    /** Retunbs the {@code terraform} command this task is implementing.
     *
     * @return Terraform command as string
     */
    @Internal
    protected String getTerraformCommand() {
        this.command
    }

    /** Adds a command-line provider.
     *
     * @param provider
     */
    protected void addCommandLineProvider(Provider<List<String>> provider) {
        this.commandLineProviders.add(provider)
    }

    /** Returns a list of the default command parameters.
     *
     * @return Default command parameters
     */
    @Internal
    protected List<String> getDefaultCommandParameters() {
        this.defaultCommandParameters
    }

    /** To be called subclass constructor for defining specific configuration extensions that are
     * supported.
     *
     * @param configExtensions
     */
    private void withConfigExtensions(List<Class> configExtensions) {
        for (Class it : configExtensions) {
            TerraformTaskConfigExtension cex = (TerraformTaskConfigExtension) it.newInstance(this)
            extensions.add(cex.name, cex)
            cex.inputProperties.eachWithIndex { Closure eval, Integer idx ->
                inputs.property "config-extension-${cex.name}-${idx}", eval
            }
            commandLineProviders.add(projectOperations.provider { -> cex.commandLineArgs })
        }
    }

    private void withTerraformConfigExtensions(
        List<TerraformExtensionConfigTypes> configExtensions
    ) {
        configExtensions.eachWithIndex { TerraformExtensionConfigTypes cfgType, Integer idx ->
            inputs.property "${TerraformExtension.NAME}-extension-${idx}", {
                -> cfgType.accessor.apply(terraformExtension).toString()
            }

            commandLineProviders.add(project.provider { ->
                cfgType.accessor.apply(terraformExtension).commandLineArgs
            })
        }
    }

    @SuppressWarnings('UnnecessaryCast')
    static private Map<String, Object> getDefaultEnvironment() {
        // tag::default-environment[]
        if (OS.windows) {
            [
                TEMP        : System.getenv('TEMP'),
                TMP         : System.getenv('TMP'),
                HOMEDRIVE   : System.getenv('HOMEDRIVE'),
                HOMEPATH    : System.getenv('HOMEPATH'),
                USERPROFILE : System.getenv('USERPROFILE'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ] as Map<String, Object>
        } else {
            [
                HOME        : System.getProperty('user.home'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ] as Map<String, Object>
        }
        // end::default-environment[]
    }

    private LogLevel terraformLogLevel
    private Object sourceSetProxy
    private boolean noProjectEnvironment = false
    private Provider<File> stdoutCapture
    private final String command
    private final List<String> defaultCommandParameters = []
    private final TerraformExtension terraformExtension
    private final List<Provider<List<String>>> commandLineProviders = []
    private final Provider<File> sourceDirProvider
    private final Provider<File> dataDirProvider
    private final Provider<File> logDirProvider
    private final Provider<File> reportsDirProvider
    private final TerraformExtension projectTerraform
    private final TerraformRCExtension terraformrc
}