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
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Transformer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.internal.TerraformConvention
import org.ysb33r.gradle.terraform.internal.TerraformUtils

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
    void setSourceSet(TerraformSourceDirectorySet source) {
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
                    NamedDomainObjectContainer<TerraformSourceDirectorySet>).getByName(
                    projectOperations.stringTools.stringize(this.sourceSetProxy)
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

    @Override
    void exec() {
        if (terraformLogLevel) {
            logDir.get().mkdirs()
        }

        TerraformUtils.terraformLogFile(name, logDir).delete()
        super.exec()
    }

    @Override
    protected TerraformExecSpec buildExecSpec() {
        TerraformExecSpec spec = super.buildExecSpec()
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
     */
    protected AbstractTerraformTask(
        String cmd,
        List<Class> configExtensions
    ) {
        super(cmd, configExtensions)

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

        projectOperations.tasks.ignoreEmptyDirectories(inputs, this.sourceFiles)
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

    protected Provider<List<String>> sourceSetVariables() {
        project.provider {
            def variables = this.sourceSet.variables
            def configExtension = variables as TerraformTaskConfigExtension
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
            TerraformConvention.taskName(sourceSet.name, command)
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

    private TerraformSourceDirectorySet sourceSetProxy
    private String terraformLogLevel = 'TRACE'
    private final Provider<File> sourceDirProvider
    private final Provider<File> dataDirProvider
    private final Provider<File> logDirProvider
    private final Provider<File> reportsDirProvider
    private final ConfigurableFileTree sourceFiles
    private final Provider<List<File>> secondarySources
}
