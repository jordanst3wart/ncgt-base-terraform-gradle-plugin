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
import groovy.transform.Synchronized
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Transformer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformMajorVersion
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.WorkspaceExtension
import org.ysb33r.gradle.terraform.config.multilevel.TerraformExtensionConfigTypes
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.internal.TerraformUtils

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_WORKSPACE
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.awsEnvironment

/** A base class for performing a {@code terraform} execution.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.1
 */
@CompileStatic
@SuppressWarnings('MethodCount')
abstract class AbstractTerraformTask extends AbstractTerraformBaseTask {

    /**
     *
     * @param source Source set of anything that can be resolved using {@link StringTools#stringize(Object s)}
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
                project.extensions.getByType(
                    NamedDomainObjectContainer<TerraformSourceDirectorySet>).getByName(projectOperations.stringTools.stringize(this.sourceSetProxy)
                )
        }
    }

    @Internal
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

    /** The level at which Terraform should log to a file.
     *
     * @return Terraform log level. Can be {@code null} signifying that logging is switched off.
     */
    @Internal
    String getLogLevel() {
        this.terraformLogLevel
    }

    /**
     * Whether to log progress to the directory specified in {@link #getLogDir}.
     *
     * @param state {@code true} to log progress
     *
     * @since 0.10.0
     */
    void setLogProgress(boolean state) {
        this.terraformLogLevel = state ? 'TRACE' : null
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

    /**
     * The workspace name.
     *
     * @return Current workspace name. {@code null} for workspace-agnostic tasks.
     *
     * @since 0.10
     */
    @Internal
    String getWorkspaceName() {
        this.workspaceName
    }

    @Override
    void exec() {
        if (switchWorkspaceBeforeExecution) {
            switchWorkspace()
        }

        if (terraformLogLevel) {
            logDir.get().mkdirs()
        }

        TerraformUtils.terraformLogFile(name, logDir).delete()

        super.exec()
    }

    @SuppressWarnings('UnnecessaryGetter')
    protected void addSessionCredentialsIfAvailable(TerraformExecSpec spec) {
        if (requiresSessionCredentials) {
            spec.environment(terraformExtension.credentialsCacheFor(
                getSourceSet().name,
                workspaceName ?: DEFAULT_WORKSPACE,
                getSourceSet().getCredentialProviders(workspaceName ?: DEFAULT_WORKSPACE)
            ))
        }
    }

    @Override
    protected TerraformExecSpec buildExecSpec() {
        TerraformExecSpec spec = super.buildExecSpec()
        addSessionCredentialsIfAvailable(spec)
        addSessionCredentialsIfAvailable(spec)
        spec
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
     * @param workspaceName Name of workspace this task is associated with. Set to {@code null} for tasks that are
     *   workspace-agnostic
     */
    protected AbstractTerraformTask(
        String cmd,
        List<Class> configExtensions,
        List<TerraformExtensionConfigTypes> terraformConfigExtensions,
        String workspaceName
    ) {
        super(cmd, configExtensions, terraformConfigExtensions)

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

        this.sourceFiles = project.fileTree(sourceDirProvider)
        this.sourceFiles.exclude('.terraform.lock.hcl', 'terraform.tfstate', '.terraform.tfstate.lock*')
        this.workspaceName = workspaceName
        this.projectName = project.name

        projectOperations.tasks.ignoreEmptyDirectories(inputs, this.sourceFiles)

        if (workspaceName != null) {
            final execSpecProvider = {
                TerraformExecSpec execSpec = createExecSpec()
                addExecutableToExecSpec(execSpec)
                Map<String, String> tfEnv = terraformEnvironment
                execSpec.identity {
                    command 'workspace'
                    workingDir sourceDir
                    environment tfEnv
                }
                execSpec.environment(environment)
                addSessionCredentialsIfAvailable(execSpec)
                execSpec
            } as Callable<TerraformExecSpec>

            workspaceController = extensions.create(
                WorkspaceExtension.NAME,
                WorkspaceExtension,
                project.provider { -> sourceSet },
                logDir,
                execSpecProvider,
                workspaceName,
                projectOperations
            )
        }
    }

    /**
     * Indicated whether this task is associated with a source set which has workspaces other than just default.
     *
     * @return {@code true} if there are workspaces. If the task is workspace-agnostic it will return {@code false}
     * even if the associated sourceset has workspaces.
     *
     * @since 0.10
     */
    @SuppressWarnings('UnnecessaryGetter')
    protected boolean hasWorkspaces() {
        workspaceController?.hasWorkspaces() ?: false
    }

    @Override
    protected Provider<File> getWorkingDirForCommand() {
        sourceDir
    }

    /**
     * Files in the source directory that act as input files to determine up to date status.
     *
     * @return File collection
     *
     * @since 0.10.0
     */
    @Internal
    protected FileCollection getSourceFiles() {
        this.sourceFiles
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
        def ws = workspaceName
        Provider<String> taskName = projectOperations.provider { ->
            TerraformConvention.taskName(sourceSet.name, command, ws)
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

    @Input
    @Override
    protected Map<String, String> getTerraformEnvironment() {
        TerraformUtils.terraformEnvironment(
            terraformrc,
            name,
            dataDir,
            logDir,
            terraformLogLevel
        )
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
    @InputFiles
    protected Provider<List<File>> getSecondarySources() {
        this.secondarySources
    }

    /**
     * Tries to determine the current terraform version group
     *
     * @return Terraform version
     *
     * @since 0.10.0
     */
    @Internal
    protected TerraformMajorVersion getTerraformMajorVersion() {
        def tssName = sourceSet?.name

        if (tssName) {
            loadTerraformVersion(tssName, terraformExtension, logger)
        } else {
            throw new TerraformConfigurationException("Source set is not associated for task ${name}")
        }
    }

    /**
     * This task is workspace aware, but workspaces should not be switched
     *
     * @since 0.10
     */
    protected void doNotSwitchWorkspace() {
        this.switchWorkspaceBeforeExecution = false
    }

    /**
     * This specific task does not interact with state and thus does not require
     * any session credentials.
     *
     * @since 0.11
     */
    protected void doesNotRequireSessionCredentials() {
        this.requiresSessionCredentials = false
    }

    /**
     * Switches workspaces to the correct one if the source set has workspaces and the current workspace is not the
     * correct one. If no additional workspace or the task is workspace agnostic, then it will do-nothing.
     *
     * @since 0.10
     */
    protected void switchWorkspace() {
        if (hasWorkspaces()) {
            workspaceController.switchWorkspace()
        }
    }

    /**
     * Runs a {@code terraform workspace} subcommand.
     *
     * @param cmd Subcommand to run.
     * @return Output from command. Returns {@code null} is workspace-agnostic.
     *
     * @since 0.10
     */
    protected String runWorkspaceSubcommand(String cmd, String... args) {
        workspaceController?.runWorkspaceSubcommand(cmd, args)
    }

    /**
     * Lists the workspaces as currently known to Terraform
     *
     * @return List of workspaces. Returns empty map if workspace-agnostic.
     */
    protected Map<String, Boolean> listWorkspaces() {
        workspaceController?.listWorkspaces() ?: [(DEFAULT_WORKSPACE): true]
    }

    @Synchronized
    private static TerraformMajorVersion loadTerraformVersion(
        String sourceSetName,
        TerraformExtension tf,
        Logger log
    ) {
        TF_VERSIONS.computeIfAbsent(sourceSetName) {
            def ver = tf.resolveTerraformVersion()
            if (ver == TerraformMajorVersion.UNKNOWN) {
                log.info('''Configured terraform version is unknown to this plugin.
  If this is a new version of terraform please raise an issue at

    https://gitlab.com/ysb33rOrg/terraform-gradle-plugin/-/issues

''')
            }
            ver
        }
    }

    private static final ConcurrentHashMap<String, TerraformMajorVersion> TF_VERSIONS =
        new ConcurrentHashMap<String, TerraformMajorVersion>()

    private Object sourceSetProxy
    private String terraformLogLevel = 'TRACE'
    private boolean switchWorkspaceBeforeExecution = true
    private boolean requiresSessionCredentials = true
    private final String workspaceName
    private final Provider<File> sourceDirProvider
    private final Provider<File> dataDirProvider
    private final Provider<File> logDirProvider
    private final Provider<File> reportsDirProvider
    private final ConfigurableFileTree sourceFiles
    private final Provider<List<File>> secondarySources
    private final String projectName
    private final WorkspaceExtension workspaceController
}
