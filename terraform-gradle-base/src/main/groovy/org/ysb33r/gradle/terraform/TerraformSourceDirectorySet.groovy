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
package org.ysb33r.gradle.terraform

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.gradle.api.Action
import org.gradle.api.Project
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
import org.ysb33r.gradle.terraform.internal.output.OutputVariablesCache
import org.ysb33r.gradle.terraform.tasks.TerraformOutput
import org.ysb33r.grolifant.api.core.ProjectOperations

import javax.inject.Inject
import java.util.concurrent.Callable
import java.util.function.Function

import static org.ysb33r.gradle.terraform.internal.DefaultTerraformTasks.OUTPUT
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
     * @param terraformRc Reference to {@link TerraformRCExtension}
     * @param name Name of source set.
     * @param displayName Display name of source set.
     */
    @Inject
    @SuppressWarnings(['ParameterCount', 'MethodSize'])
    TerraformSourceDirectorySet(
        Project tempProjectReference,
        ObjectFactory objects,
        TaskContainer tasks,
        TerraformRCExtension terraformRc,
        String name,
        String displayName
    ) {
        this.projectOperations = ProjectOperations.create(tempProjectReference)
        this.objectFactory = tempProjectReference.objects
        this.name = name
        this.displayName = displayName
        this.patternSet.include('**/*.tf', '**/*.tfvars', '*.tfstate')

        sourceDir = objects.property(File)
        dataDir = objects.property(File)
        logDir = objects.property(File)
        reportsDir = objects.property(File)

        projectOperations.fsOperations.updateFileProperty(
            sourceDir,
            "src/tf/${name}"
        )

        projectOperations.fsOperations.updateFileProperty(
            dataDir,
            projectOperations.buildDirDescendant("tf/${name}")
        )

        projectOperations.fsOperations.updateFileProperty(
            logDir,
            projectOperations.buildDirDescendant("tf/${name}/logs")
        )

        projectOperations.fsOperations.updateFileProperty(
            reportsDir,
            projectOperations.buildDirDescendant("reports/tf/${name}")
        )

        outputVariablesProvider = createOutputVariablesProvider(
            terraformRc,
            projectOperations,
            objectFactory,
            tasks,
            name
        )

        vars = new Variables(this.sourceDir)

        this.closureCleaner = { Project project, Object varsContainer, Closure cfg ->
            Closure cleaned = ((Closure) cfg.clone()).dehydrate().rehydrate(varsContainer, project, project)
            cleaned.resolveStrategy = Closure.DELEGATE_FIRST
            cleaned
        }.curry(tempProjectReference, this.vars) as Function<Closure, Closure>

        this.secondarySources = []
        this.secondarySourcesProvider = projectOperations.provider({ List<Object> files ->
            projectOperations.fsOperations.files(files).toList()
        }.curry(this.secondarySources))
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
        projectOperations.fsOperations.updateFileProperty(this.sourceDir, dir)
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
        projectOperations.fsOperations.updateFileProperty(this.dataDir, dir)
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
        projectOperations.fsOperations.updateFileProperty(this.logDir, dir)
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
        projectOperations.fsOperations.updateFileProperty(this.reportsDir, dir)
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

    /** Get all terraform variables applicable to this source set.
     *
     * @param cfg
     *
     * @since 0.2
     */
    VariablesSpec getVariables() {
        this.vars
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

    @SuppressWarnings('ParameterCount')
    private static Provider<Map<String, ?>> createOutputVariablesProvider(
        TerraformRCExtension terraformrc,
        ProjectOperations projectOperations,
        ObjectFactory objectFactory1,
        TaskContainer tasks,
        String sourceSetName
    ) {
        String outputTaskName = taskName(sourceSetName, OUTPUT.command)

        def outputTaskProvider = projectOperations.provider {
            (TerraformOutput) tasks.named(outputTaskName)
        }

        def cache = new OutputVariablesCache(
            projectOperations,
            terraformrc,
            outputTaskProvider
        )

        createProperty(
            objectFactory1,
            projectOperations.provider({ ->
                cache.map
            } as Callable<Map<String, ?>>)) as Provider<Map<String, ?>>
    }

    @CompileDynamic
    @TypeChecked
    private static Provider<Map<String, Object>> createProperty(
        ObjectFactory objectFactory1,
        Provider<Map<String, ?>> lambda
    ) {
        def prop = objectFactory1.mapProperty(String, Object)
        prop.disallowUnsafeRead()
        prop.set(lambda)
        prop
    }

    private final Property<File> sourceDir
    private final Property<File> dataDir
    private final Property<File> logDir
    private final Property<File> reportsDir
    private final ProjectOperations projectOperations
    private final ObjectFactory objectFactory
    private final Variables vars
    private final Provider<Map<String, ?>> outputVariablesProvider
    private final PatternSet patternSet = new PatternSet()
    private final Function<Closure, Closure> closureCleaner
    private final List<Object> secondarySources
    private final Provider<List<File>> secondarySourcesProvider
}