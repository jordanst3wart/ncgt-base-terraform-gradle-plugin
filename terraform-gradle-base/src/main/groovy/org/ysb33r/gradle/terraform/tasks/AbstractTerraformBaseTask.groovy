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

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecSpec
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.internal.TerraformConfigUtils
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.AbstractExecSpec

import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.createPluginCacheDir

/**
 * Base class for Terraform tasks.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.10
 */
@CompileStatic
abstract class AbstractTerraformBaseTask extends DefaultTask {

    protected AbstractTerraformBaseTask(
        String cmd,
        List<Class> configExtensions
    ) {
        this.projectOperations = ProjectOperations.find(project)
        this.projectTerraform = project.extensions.getByType(TerraformExtension)
        this.terraformrc = TerraformConfigUtils.locateTerraformRCExtension(project)
        this.command = cmd
        withConfigExtensions(configExtensions)
    }

    /** Replace current environment with new one.
     *
     * Calling this will also remove any project extension environment from this task.
     *
     * @param args New environment key-value map of properties.
     */
    // @Override
    void setEnvironment(Map<String, ?> args) {
        this.env.putAll((Map<String, Object>) args)
    }

    /** Environment for running the exe
     *
     * <p> Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    // @Override
    Map<String, String> getEnvironment() {
        this.env as Map<String, String>
    }

    /**
     * Obtain a list of associated variables, should if be a valid condition for the task.
     *
     * In most cases this will be empty.
     *
     * @return Associated variables in terraform format.
     *
     * @since 0.13
     */
    @Internal
    List<Provider<List<String>>> getTfVarProviders() {
        this.tfVarProviders
    }

    // @Override
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
    protected TerraformRCExtension getTerraformrc() {
        this.terraformrc
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

    // this method should be collapsed into the getTerraformEnvironment method
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

    protected TerraformExecSpec buildExecSpec() {
        TerraformExecSpec execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        configureExecSpec(execSpec)
    }

    /** Creates a {@link TerraformExecSpec}.
     *
     * @return {@link TerraformExecSpec}. Never {@code null}.
     */
    // @Override
    protected TerraformExecSpec createExecSpec() {
        new TerraformExecSpec(projectOperations, projectTerraform.resolver)
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
    // @Override
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

    // @Override
    @Internal
    protected TerraformExtension getToolExtension() {
        projectTerraform
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
        for (it in configExtensions) {
            TerraformTaskConfigExtension cex = (TerraformTaskConfigExtension) project.objects.newInstance(it)
            extensions.add(cex.name, cex)
            commandLineProviders.add(projectOperations.provider { -> cex.commandLineArgs })
        }
    }

    protected TerraformExecSpec addExecutableToExecSpec(final TerraformExecSpec execSpec) {
        execSpec.executable(toolExtension.resolvableExecutable.executable.absolutePath)
        return execSpec
    }

    private final String command
    private final ProjectOperations projectOperations
    private final TerraformExtension projectTerraform
    private final TerraformRCExtension terraformrc
    private final List<Provider<List<String>>> commandLineProviders = []
    private final List<Provider<List<String>>> tfVarProviders = []
    private final List<String> defaultCommandParameters = []
    private Provider<File> stdoutCapture
    private final Map<String, Object> env = [:]
}
