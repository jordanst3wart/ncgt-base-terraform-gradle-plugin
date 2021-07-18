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
package org.ysb33r.gradle.terraform.config.multilevel

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.config.TerraformTaskConfigExtension
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import org.ysb33r.grolifant.api.v4.StringUtils

import javax.inject.Inject
import java.nio.file.Path
import java.util.stream.Collectors

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedList
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedMap
import static org.ysb33r.grolifant.api.v4.MapUtils.stringizeValues

/** A configuration building block for tasks that need to pass variables to
 * a {@code terraform task}.
 *
 * @since 0.1
 */
@CompileStatic
class Variables implements TerraformTaskConfigExtension,
    VariablesSpec, TerraformExtensionEmbeddable, TerraformSourceSetEmbeddable {

    final String name = 'variables'

    /** Attach this configuration block to a Terraform extension or source directory set
     *
     * @param rootFileResolver Root file resolver for file that are referenced.
     *
     * @since 0.2
     */
    Variables(Provider<File> rootFileResolver) {
        this.rootDirResolver = rootFileResolver
    }

    /** Constructs instance from definition of files and variables
     *
     * @param vfp Definition of files and variables
     * @param rootFileResolver Root file resolver for file that are referenced.
     *
     * @since 0.2
     */
    Variables(VarsFilesPair vfp, Provider<File> rootFileResolver) {
        this.varsFilesPair.files.addAll(vfp.files)
        this.varsFilesPair.vars.putAll(vfp.vars)
        this.rootDirResolver = rootFileResolver
    }

    /** Adds one variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param value Lazy-evaluated form of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize(Object)}
     * is accepted.
     */
    @Override
    void var(final String name, final Object value) {
        varsFilesPair.vars.put(name, value)
    }

    /** Adds a map as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param map Lazy-evaluated form of map.
     *  Anything resolvable via {@link org.ysb33r.grolifant.api.v4.MapUtils#stringizeValues(Map)}
     * is accepted.
     */
    @Override
    void map(Map<String, ?> map, final String name) {
        varsFilesPair.vars.put(name, map)
    }

    /**
     * Adds a map provider as a variable.
     *
     * <p> This will replace any previous map by the same name.
     *
     * @param name Name of map
     * @param mapProvider Provider to map
     */
    @Override
    void map(String name, Provider<Map<String, ?>> mapProvider) {
        varsFilesPair.vars.put(name, mapProvider)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    @Override
    void list(final String name, Object val1, Object... vals) {
        List<Object> inputs = [val1]
        inputs.addAll(vals)
        varsFilesPair.vars.put(name, inputs)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    @Override
    void list(final String name, Iterable<?> vals) {
        varsFilesPair.vars.put(name, vals as List)
    }

    /** Adds a name of a file containing {@code terraform} variables.
     *
     * @param fileName Files that can be converted via
     * {@link org.ysb33r.grolifant.api.v4.StringUtils#stringize(Object o)} and resolved relative to the appropriate
     * {@link org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}.
     */
    @Override
    void file(final Object fileName) {
        varsFilesPair.files.add fileName
    }

    /** Removes all existing variables and file references.
     *
     */
    void clear() {
        varsFilesPair.clear()
    }

    /** Evaluate all provided and loca; variables and convert them to Terraform-compliant strings, ready to be
     * passed to command-line.
     *
     * Provided variables will be evaluated first, so that any local definitions can override them.
     *
     * <p> Calling this will resolve all lazy-evaluated entries.
     *
     * @return Map where each key is the name of a variable. Each value is correctly formatted according to
     *   the kind of variable.
     */
    Map<String, String> getEscapedVars() {
        Map<String, String> hclMap = escapeProvidedVars()
        hclMap.putAll(escapeLocalVars())
        hclMap
    }

    /** List of file names containing Terraform variables.
     *
     * Filenames can contain relative paths.
     *
     * @return List of filenames.
     */
    Set<String> getFileNames() {
        StringUtils.stringize(this.varsFilesPair.files).toSet()
    }

    @Override
    @SuppressWarnings('UnnecessaryCast')
    List<Closure> getInputProperties() {
        [
            { Map m ->
                stringizeValues(m)
            }.curry(this.varsFilesPair.vars),
            { ->
                fileNames
            }
        ] as List<Closure>
    }

    @Override
    List<String> getCommandLineArgs() {
        Path root = rootDirResolver.orNull?.toPath()
        if (root == null) {
            throw new TerraformConfigurationException(
                'This method can only be called when attached to a task extension or a source set'
            )
        }

        final List<String> varList = escapedVars.collectMany { String k, String v ->
            ['-var', "$k=$v".toString()]
        } as List<String>
        varList.addAll(fileNames.stream().map { String fileName ->
            "-var-file=${root.resolve(fileName).toFile().absolutePath}".toString()
        }.collect(Collectors.toList()) as List<String>)
        varList
    }

    @Override
    String toString() {
        "Terraform variables: ${this.varsFilesPair.toString()}"
    }

    /** Returns a description of the files and variables
     *
     * @return Files containing variables as well as explicitly declared variabled/
     *
     * @since 0.2
     */
    VarsFilesPair getAllVars() {
        this.varsFilesPair
    }

    /**
     * Adds additional actions which can add variables. These will be called first when evaluating a final escaped
     * variable map.
     *
     * @param additionalVariables Action that can be called to provide additional variables.
     *
     * @since 0.12
     */
    @Override
    void provider(Action<VariablesSpec> additionalVariables) {
        this.additionalVariables.add(additionalVariables)
    }

    private Map<String, String> escapeProvidedVars() {
        def vars = new Variables(new VarsFilesPair(), rootDirResolver)
        for (Action<VariablesSpec> additional : this.additionalVariables) {
            additional.execute(vars)
        }
        vars.escapeLocalVars()
    }

    private Map<String, String> escapeLocalVars() {
        Map<String, String> hclMap = [:]
        for (String key in this.varsFilesPair.vars.keySet()) {
            hclMap[key] = escapeObject(this.varsFilesPair.vars[key])
        }
        hclMap
    }

    private String escapeObject(Object variable) {
        switch (variable) {
            case Provider:
                return escapeObject(((Provider) variable).get())
            case Map:
                return escapedMap((Map) variable)
            case List:
                return escapedList((Iterable) variable)
            default:
                return StringUtils.stringize(variable)
        }
    }

    private final VarsFilesPair varsFilesPair = new VarsFilesPair()
    private final Provider<File> rootDirResolver
    private final List<Action<VariablesSpec>> additionalVariables = []

    @Inject
    Project project
}
