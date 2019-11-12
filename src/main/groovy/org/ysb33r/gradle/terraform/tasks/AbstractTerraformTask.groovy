/*
 * Copyright 2017-2019 the original author or authors.
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

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.TerraformSourceSets
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.grolifant.api.StringUtils
import org.ysb33r.grolifant.api.exec.AbstractExecWrapperTask

import java.util.concurrent.Callable

import static org.ysb33r.gradle.terraform.internal.Downloader.OS
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.createPluginCacheDir
import static org.ysb33r.grolifant.api.StringUtils.stringize

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
        sourceSet?.srcDir ?: project.provider({ ->
            sourceSet.srcDir.get()
        } as Callable<File>)
    }

    @OutputDirectory
    Provider<File> getDataDir() {
        sourceSet?.dataDir ?: project.provider({ ->
            sourceSet.dataDir.get()
        } as Callable<File>)
    }

    @Internal
    Provider<File> getLogDir() {
        sourceSet?.logDir ?: project.provider({ ->
            sourceSet.logDir.get()
        } as Callable<File>)
    }

    @Internal
    Provider<File> getReportsDir() {
        sourceSet?.reportsDir ?: project.provider({ ->
            sourceSet.reportsDir.get()
        } as Callable<File>)
    }

    /** The level at which Terraform should log.
     *
     * @return Terraform log level. Can be {@code null} signifying that logging is switched off.
     */
    @Internal
    String getLogLevel() {
        switch (this.terraformLogLevel ?: project.logging.level) {
            case LogLevel.DEBUG:
                return 'DEBUG'
            case LogLevel.ERROR:
                return 'ERROR'
            case LogLevel.WARN:
                return 'WARN'
            case LogLevel.INFO:
                return 'INFO'
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
            combinedEnv.putAll(project.extensions.getByType(TerraformExtension).environment)
            combinedEnv.putAll(super.environment)
            combinedEnv
        }
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    String terraformPath(Object file) {
        TerraformUtils.terraformPath(project, file)
    }

    @Override
    void exec() {
        TerraformExecSpec execSpec = buildExecSpec()
        createPluginCacheDir(project)

        if (logLevel) {
            logDir.get().mkdirs()
        }

        ExecResult result = project.exec(new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec spec) {
                execSpec.copyToExecSpec(spec)
            }
        })
        result.assertNormalExitValue()
    }

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
        this.command = cmd
        terraformExtension = extensions.create(
            TerraformExtension.NAME,
            TerraformExtension,
            this,
            terraformConfigExtensions
        )
        withConfigExtensions(configExtensions)
        withTerraformConfigExtensions(terraformConfigExtensions)
        environment(defaultEnvironment)
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
     */
    protected void supportsColor() {
        ConsoleOutput mode = project.gradle.startParameter.consoleOutput
        if (mode == ConsoleOutput.Plain || mode == ConsoleOutput.Auto && System.getenv('TERM') == 'dumb') {
            defaultCommandParameters.add '-no-color'
        }
    }

    protected void supportsForce() {
        if (project.gradle.startParameter.rerunTasks) {
            defaultCommandParameters.add '-force'
        }
    }

    // Internal method used for testing
    @SuppressWarnings('PublicMethodsBeforeNonPublicMethods')
    @PackageScope
    TerraformExecSpec buildExecSpec() {
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
        new TerraformExecSpec(project, toolExtension.resolver)
    }

    /** Configures a {@link TerraformExecSpec}.
     *
     * @param execSpec Specification to be configured
     * @return Conrfigured specification
     */
    @Override
    @SuppressWarnings('DuplicateStringLiteral')
    protected TerraformExecSpec configureExecSpec(TerraformExecSpec execSpec) {
        final String tfcmd = terraformCommand
        Map<String, String> tfEnv = terraformEnvironment
        logger.info "Using Terraform environment: ${tfEnv}"
        execSpec.identity {
            command tfcmd
            workingDir sourceDir
            environment tfEnv
            cmdArgs defaultCommandParameters
            addCommandSpecificsToExecSpec(execSpec)
        }

        execSpec.environment(environment)
        execSpec
    }

    @SuppressWarnings('DuplicateStringLiteral')
    @Input
    protected Map<String, String> getTerraformEnvironment() {
        final Map<String, String> env = [
            TF_DATA_DIR       : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE: TerraformConfigUtils.locateTerraformConfigFile(project).absolutePath,
            TF_LOG_PATH       : new File(logDir.get(), "${name}.log").absolutePath,
            TF_LOG            : logLevel ?: ''
        ]

        env
    }

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

//    protected void addModuleDepthToCmdLine(TerraformExecSpec execSpec, int moduleDepth) {
//        execSpec.cmdArgs "-module-depth=${moduleDepth}"
//    }

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

    /** To be called subclass constructor for defining specific configuration extensions that are
     * supported.
     *
     * @param configExtensions
     */
    private void withConfigExtensions(List<Class> configExtensions) {
        for (Class it : configExtensions) {
            TerraformTaskConfigExtension cex = it.newInstance(this)
            extensions.add(cex.name, cex)
            cex.inputProperties.eachWithIndex { Closure eval, Integer idx ->
                inputs.property "config-extension-${cex.name}-${idx}", eval
            }
            commandLineProviders.add(project.provider { -> cex.commandLineArgs })
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
                (OS.pathVar): System.getenv(OS.pathVar)
            ] as Map<String, Object>
        }
        // end::default-environment[]
    }

    private LogLevel terraformLogLevel
    private Object sourceSetProxy
    private boolean noProjectEnvironment = false
    private final String command
    private final List<String> defaultCommandParameters = []
    private final TerraformExtension terraformExtension
    private final List<Provider<List<String>>> commandLineProviders = []

}