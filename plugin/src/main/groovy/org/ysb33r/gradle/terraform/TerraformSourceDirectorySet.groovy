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

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.ysb33r.gradle.terraform.config.VariableSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.grolifant.api.core.ProjectOperations

import javax.inject.Inject

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
        Project project,
        String name,
        String displayName
    ) {
        this.projectOperations = ProjectOperations.maybeCreateExtension(project)
        this.name = name
        this.displayName = displayName
        this.patternSet.include('**/*.tf', '**/*.tfvars', '*.tfstate')

        sourceDir = project.objects.property(File)
        dataDir = project.objects.property(File)
        logDir = project.objects.property(File)
        reportsDir = project.objects.property(File)
        backendText = project.objects.property(String)

        projectOperations.fsOperations.updateFileProperty(
            sourceDir,
            "src/${name}/tf"
        )

        projectOperations.fsOperations.updateFileProperty(
            dataDir,
            projectOperations.buildDirDescendant("${name}/tf")
        )

        projectOperations.fsOperations.updateFileProperty(
            logDir,
            projectOperations.buildDirDescendant("${name}/tf/logs")
        )

        projectOperations.fsOperations.updateFileProperty(
            reportsDir,
            projectOperations.buildDirDescendant("${name}/tf/reports")
        )

        this.vars = new Variables(this.sourceDir)
        this.secondarySources = []
        this.secondarySourcesProvider = projectOperations.provider({ List<Object> files ->
            projectOperations.fsOperations.files(files).toList()
        }.curry(this.secondarySources))
    }

    @Override
    String toString() {
        this.displayName
    }

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

    void setBackendText(String backText) {
        this.backendText.set(backText)
    }

    Property<String> backendPropertyText() {
        backendText
    }

    /** Data directory provider.
     *
     * @return File provider.
     */
    Provider<File> getDataDir() {
        this.dataDir
    }

    /** Log directory provider.
     *
     * @return File provider.
     */
    Provider<File> getLogDir() {
        this.logDir
    }

    /** Reports directory.
     *
     * @return File provider.
     */
    Provider<File> getReportsDir() {
        this.reportsDir
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
     * @param cfg Configurating action.
     *
     * @since 0.2
     */
    void variables(Action<VariableSpec> cfg) {
        cfg.execute(this.vars)
    }

    /** Get all terraform variables applicable to this source set.
     *
     */
    VariableSpec getVariables() {
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

    private final Property<String> backendText
    private final Property<File> sourceDir
    private final Property<File> dataDir
    private final Property<File> logDir
    private final Property<File> reportsDir
    private final ProjectOperations projectOperations
    private final Variables vars
    private final PatternSet patternSet = new PatternSet()
    private final List<Object> secondarySources
    private final Provider<List<File>> secondarySourcesProvider
}