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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.gradle.terraform.internal.TerraformUtils
import org.ysb33r.gradle.terraform.internal.output.OutputVariablesCache
import org.ysb33r.gradle.terraform.tasks.TerraformOutput
import org.ysb33r.grolifant.api.core.LegacyLevel
import org.ysb33r.grolifant.api.core.ProjectOperations

import javax.inject.Inject
import java.util.concurrent.Callable
import java.util.function.BiFunction
import java.util.function.Function

import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.OUTPUT
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.DEFAULT_WORKSPACE
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.createWorkspaceTasksByConvention
import static org.ysb33r.gradle.terraform.internal.TerraformConvention.taskName

/** Describes a Terraform source set
 *
 */
@CompileStatic
@SuppressWarnings('MethodCount')
class TerraformSourceDirectorySet implements PatternFilterable {

    /** Source directory name
     *
     */
    final String name

    /** Displayable name of source set.
     *
     */
    final String displayName

    /** Constructs the source set.
     *
     * @param tempProjectReference Project this source set is attached to.
     * @param object Object factory
     * @param tasks Take container
     * @param terraformrc Reference to {@link TerraformRCExtension}
     * @param name Name of source set.
     * @param displayName Display name of source set.
     */
    @Inject
    @SuppressWarnings('ParameterCount')
    TerraformSourceDirectorySet(
        Project tempProjectReference,
        ObjectFactory objects,
        TaskContainer tasks,
        TerraformRCExtension terraformrc,
        String name,
        String displayName
    ) {
        this.projectOperations = ProjectOperations.create(tempProjectReference)
        this.name = name
        this.displayName = displayName
        this.patternSet.include('**/*.tf', '**/*.tfvars', '*.tfstate')
        this.outputVariablesProviderMap = [:]

        this.outputVariablesProviderFunction = new BiFunction<String, String, Provider<Map<String, ?>>>() {
            @Override
            Provider<Map<String, ?>> apply(String sourceSetName, String workspaceName) {
                createOutputVariablesProvider(terraformrc, projectOperations, tasks, sourceSetName, workspaceName)
            }
        }

        sourceDir = objects.property(File)
        dataDir = objects.property(File)
        logDir = objects.property(File)
        reportsDir = objects.property(File)

        projectOperations.updateFileProperty(
            sourceDir,
            "src/tf/${name}"
        )

        projectOperations.updateFileProperty(
            dataDir,
            projectOperations.buildDirDescendant("tf/${name}")
        )

        projectOperations.updateFileProperty(
            logDir,
            projectOperations.buildDirDescendant("tf/${name}/logs")
        )

        projectOperations.updateFileProperty(
            reportsDir,
            projectOperations.buildDirDescendant("reports/tf/${name}")
        )

        outputVariablesProviderMap[DEFAULT_WORKSPACE] = outputVariablesProviderFunction.apply(name, DEFAULT_WORKSPACE)

        vars = new Variables(this.sourceDir)

        this.closureCleaner = { Project project, Object varsContainer, Closure cfg ->
            Closure cleaned = ((Closure) cfg.clone()).dehydrate().rehydrate(varsContainer, project, project)
            cleaned.resolveStrategy = Closure.DELEGATE_FIRST
            cleaned
        }.curry(tempProjectReference, this.vars) as Function<Closure, Closure>

        this.secondarySources = []
        this.secondarySourcesProvider = projectOperations.provider({ List<Object> files ->
            projectOperations.fileize(files)
        }.curry(this.secondarySources))

        this.workspaces = createWorkspaceContainer(tempProjectReference, objects)
        def xref = this
        this.workspaces.all(new Action<Workspace>() {
            @Override
            void execute(Workspace ws) {
                createWorkspaceTasksByConvention(tempProjectReference, xref, ws.name)
            }
        })
    }

    /** The display name is the string representation of the source set.
     *
     * @return String representation
     */
    @Override
    String toString() {
        this.displayName
    }

    /** Provide for the root directory of the source set,
     *
     * @return File provider.
     */
    Provider<File> getSrcDir() {
        this.sourceDir
    }

    /** Sets the source directory.
     *
     * @param dir Directory can be anything convertible using {@link Project#file}.
     * @return {@code this}.
     */
    void setSrcDir(Object dir) {
        projectOperations.updateFileProperty(this.sourceDir, dir)
    }

    /** Data directory provider.
     *
     * @return File provider.
     */
    Provider<File> getDataDir() {
        this.dataDir
    }

    /** Sets the Terraform data directory.
     *
     * @param dir Directory can be anything convertible using {@link Project#file}.
     * @return {@code this}.
     */
    void setDataDir(Object dir) {
        projectOperations.updateFileProperty(this.dataDir, dir)
    }

    /** Log directory provider.
     *
     * @return File provider.
     */
    Provider<File> getLogDir() {
        this.logDir
    }

    /** Sets the log directory.
     *
     * @param dir Directory can be anything convertible using {@link Project#file}.
     * @return {@code this}.
     */
    void setLogDir(Object dir) {
        projectOperations.updateFileProperty(this.logDir, dir)
    }

    /** Reports directory.
     *
     * @return File provider.
     */
    Provider<File> getReportsDir() {
        this.reportsDir
    }

    /** Sets the reports directory.
     *
     * @param dir Directory can be anything convertible using {@link Project#file}.
     * @return {@code this}.
     */
    void setReportsDir(Object dir) {
        projectOperations.updateFileProperty(this.reportsDir, dir)
    }

    /** Returns the pattern filter.
     *
     * @return Terraform pattern filter.
     */
    PatternFilterable getFilter() {
        this.patternSet
    }

    /** Returns terraform source tree
     *
     * @return Source tree as a file tree.
     */
    FileTree getAsFileTree() {
        projectOperations.fileTree(sourceDir).matching(this.patternSet)
    }

    /**
     * Additional sources that affects infrastructure.
     *
     * @param files Anything convertible to a file.
     *
     * @since 0.10.
     */
    void secondarySources(Object... files) {
        this.secondarySources.addAll(files)
    }
    /**
     * Additional sources that affects infrastructure.
     *
     * @param files Anything convertible to a file.
     *
     * @since 0.10.
     */
    void secondarySources(Iterable<Object> files) {
        this.secondarySources.addAll(files)
    }

    /** Provides a list of secondary sources.
     *
     * @return Provider never returns null, but could return an empty list.
     */
    Provider<List<File>> getSecondarySources() {
        this.secondarySourcesProvider
    }

    /** Sets Terraform variables that are applicable to this source set.
     *
     * @param cfg Closure that configures a {@link VariablesSpec}.
     *
     * @since 0.2
     */
    void variables(@DelegatesTo(VariablesSpec) Closure cfg) {
        Closure runner = closureCleaner.apply(cfg)
        runner()
    }

    /** Sets Terraform variables that are applicable to this source set.
     *
     * @param cfg Configurating action.
     *
     * @since 0.2
     */
    void variables(Action<VariablesSpec> cfg) {
        cfg.execute(this.vars)
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

    /** Get all terraform variables applicable to this source set.
     *
     * @param cfg
     *
     * @since 0.2
     */
    VariablesSpec getVariables() {
        this.vars
    }

    /** Returns a provider which can be used to access output variables from a source set.
     *
     * Invoking the provider return by this call will invoke {@code terraform output} the first time, but thereafter
     * values will be cached for the remainder of of the build. If you want updates values, ensure the relavent
     * {@link org.ysb33r.gradle.terraform.tasks.TerraformApply} task is run before invoking the provider.
     *
     * @param workspaceName which workspace this is for. Defaults to {@code default} workspace if not supplied.
     *
     * @return Provider for reading output values. THe values are returned in a raw format which basically is just the
     *   JSON output parsed into some form of map.
     *
     * @since 0.9.0
     */
    Provider<Map<String, ?>> getRawOutputVariables(String workspaceName = DEFAULT_WORKSPACE) {
        def provider = this.outputVariablesProviderMap[workspaceName]

        if (provider == null) {
            projectOperations.provider({ Map<String, Provider<Map<String, ?>>> map ->
                def p = this.outputVariablesProviderMap[workspaceName]
                if (p == null) {
                    throw new TerraformConfigurationException("Requested workspace ${workspaceName} was not defined")
                } else {
                    p.get()
                }
            }.curry(this.outputVariablesProviderMap) as Callable<Map<String, ?>>)
        } else {
            provider
        }
    }

    /** Returns a provider to a specific output variable.
     *
     * Invoking the provider return by this call will invoke {@code terraform output} the first time, but thereafter
     * values will be cached for the remainder of of the build. If you want updates values, ensure the relavent
     * {@link org.ysb33r.gradle.terraform.tasks.TerraformApply} task is run before invoking the provider.
     *
     * @param varName Name of specific variable.
     * @param workspaceName which workspace this is for. Defaults to {@code default} workspace if not supplied.
     * @return Provider for value of specific output variable. If the variable is not a primitive type, it is up to
     * the caller to do further processing,
     *
     * @since 0.9.0
     */
    Provider<Object> rawOutputVariable(String varName, String workspaceName = DEFAULT_WORKSPACE) {
        getRawOutputVariables(workspaceName).map(new Transformer<Object, Map<String, ?>>() {
            @Override
            Object transform(Map<String, ?> stringMap) {
                stringMap[varName]['value']
            }
        })
    }

    @Override
    PatternFilterable exclude(Closure closure) {
        patternSet.exclude(closure)
    }

    @Override
    PatternFilterable exclude(Spec<FileTreeElement> spec) {
        patternSet.exclude(spec)
    }

    @Override
    PatternFilterable exclude(String... strings) {
        patternSet.exclude(strings)
    }

    @Override
    PatternFilterable exclude(Iterable<String> iterable) {
        patternSet.exclude(iterable)
    }

    @Override
    Set<String> getIncludes() {
        patternSet.includes
    }

    @Override
    Set<String> getExcludes() {
        patternSet.excludes
    }

    @Override
    PatternFilterable setIncludes(Iterable<String> iterable) {
        patternSet.includes = iterable
        this
    }

    @Override
    PatternFilterable setExcludes(Iterable<String> iterable) {
        patternSet.excludes = iterable
        this
    }

    @Override
    PatternFilterable include(String... strings) {
        patternSet.include(strings)
    }

    @Override
    PatternFilterable include(Iterable<String> iterable) {
        patternSet.include(iterable)
    }

    @Override
    PatternFilterable include(Spec<FileTreeElement> spec) {
        patternSet.include(spec)
    }

    @Override
    PatternFilterable include(Closure closure) {
        patternSet.include(closure)
    }

    /**
     * Adds one or more workspaces.
     *
     * @since 0.10.0
     */
    void workspaces(String... workspaceNames) {
        for (String ws : workspaceNames) {
            this.workspaces.create(ws)

            outputVariablesProviderFunction.apply(name, ws)
        }
    }

    /**
     * List of additional workspaces.
     *
     * @return Names of workspaces other than {@code default}.
     *
     * @since 0.10.0
     */
    List<String> getWorkspaceNames() {
        this.workspaces*.name
    }

    /**
     * Flags whether workspaces have been defined.
     *
     * @return {@code true} is one or more workspaces have been defined
     *
     * @since 0.10.0
     */
    boolean hasWorkspaces() {
        !this.workspaces.empty
    }

    static class Workspace implements Named {
        final String name

        Workspace(String n) {
            this.name = n
        }
    }

    @CompileDynamic
    private static NamedDomainObjectContainer<Workspace> createWorkspaceContainer(
        Project tempProjectReference,
        ObjectFactory objects
    ) {
        if (LegacyLevel.PRE_5_5) {
            tempProjectReference.container(Workspace)
        } else {
            objects.domainObjectContainer(Workspace)
        }
    }

    private static Provider<Map<String, ?>> createOutputVariablesProvider(
        TerraformRCExtension terraformrc,
        ProjectOperations projectOperations,
        TaskContainer tasks,
        String sourceSetName,
        String workspaceName
    ) {
        String outputTaskName = taskName(sourceSetName, OUTPUT.command, workspaceName)

        def outputTaskProvider = projectOperations.provider {
            (TerraformOutput) tasks.getByName(outputTaskName)
        }

        def cache = new OutputVariablesCache(
            projectOperations,
            terraformrc,
            outputTaskProvider
        )

        projectOperations.provider({ ->
            cache.map
        } as Callable<Map<String, ?>>)
    }

    private final Property<File> sourceDir
    private final Property<File> dataDir
    private final Property<File> logDir
    private final Property<File> reportsDir
    private final ProjectOperations projectOperations
    private final Variables vars
    private final Map<String, Provider<Map<String, ?>>> outputVariablesProviderMap
    private final PatternSet patternSet = new PatternSet()
    private final Function<Closure, Closure> closureCleaner
    private final List<Object> secondarySources
    private final Provider<List<File>> secondarySourcesProvider
    private final NamedDomainObjectContainer<Workspace> workspaces
    private final BiFunction<String, String, Provider<Map<String, ?>>> outputVariablesProviderFunction
}