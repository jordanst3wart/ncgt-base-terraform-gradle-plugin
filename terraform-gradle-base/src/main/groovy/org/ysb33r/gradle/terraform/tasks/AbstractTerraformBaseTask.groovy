/*
 * Copyright 2017-2021 the original author or authors.
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
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.AbstractExecWrapperTask

import static org.ysb33r.gradle.terraform.internal.Downloader.OS
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.createPluginCacheDir

/**
 * Base class for Terraform tasks.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
@CompileStatic
abstract class AbstractTerraformBaseTask extends AbstractExecWrapperTask<TerraformExecSpec, TerraformExtension> {

    @SuppressWarnings('UnnecessaryCast')
    static Map<String, Object> getDefaultEnvironment() {
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
        logger.info "Using Terraform environment: ${terraformEnvironment}"
        logger.debug "Terraform executable will be launched with environment: ${execSpec.environment}"
        if (this.stdoutCapture) {
            this.stdoutCapture.get().withOutputStream { strm ->
                execSpec.standardOutput(strm)
                projectOperations.exec(runner).assertNormalExitValue()
            }
        } else {
            projectOperations.exec(runner).assertNormalExitValue()
        }
    }

    @Internal
    protected TerraformExtension getProjectTerraform() {
        this.projectTerraform
    }

    @Internal
    protected TerraformRCExtension getTerraformrc() {
        this.terraformrc
    }

    @Internal
    protected TerraformExtension getTerraformExtension() {
        this.terraformExtension
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

    protected AbstractTerraformBaseTask(
        String cmd,
        List<Class> configExtensions,
        List<TerraformExtensionConfigTypes> terraformConfigExtensions
    ) {
        this.projectOperations = ProjectOperations.find(project)
        this.projectTerraform = project.extensions.getByType(TerraformExtension)
        this.terraformrc = TerraformConfigUtils.locateTerraformRCExtension(project)
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

    @Input
    abstract protected Map<String, String> getTerraformEnvironment()

    @Internal
    abstract protected Provider<File> getWorkingDirForCommand()

    /** When command is run, capture the standard output
     *
     * @param output Output file
     */
    protected void captureStdOutTo(Provider<File> output) {
        this.stdoutCapture = output
    }

    @Override
    @Internal
    protected TerraformExtension getToolExtension() {
        this.terraformExtension
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

    /** Returns the {@code terraform} command this task is implementing.
     *
     * @return Terraform command as string
     */
    @Internal
    protected String getTerraformCommand() {
        this.command
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

     /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        execSpec.cmdArgs(commandLineProviders*.get().flatten())
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
            workingDir workingDirForCommand
            environment tfEnv
            cmdArgs cmdParams
        }

        execSpec.environment(environment)
        execSpec
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

    private final String command
    private final ProjectOperations projectOperations
    private final TerraformExtension projectTerraform
    private final TerraformRCExtension terraformrc
    private final TerraformExtension terraformExtension
    private final List<Provider<List<String>>> commandLineProviders = []
    private final List<String> defaultCommandParameters = []
    private boolean noProjectEnvironment = false
    private Provider<File> stdoutCapture
}
