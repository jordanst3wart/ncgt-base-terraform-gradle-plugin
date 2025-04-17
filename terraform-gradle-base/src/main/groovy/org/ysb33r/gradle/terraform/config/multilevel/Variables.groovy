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
package org.ysb33r.gradle.terraform.config.multilevel

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.gradle.terraform.errors.TerraformConfigurationException
import java.nio.file.Path

import static org.ysb33r.grolifant.api.v4.MapUtils.stringizeValues

/** A configuration building block for tasks that need to pass variables to
 * a {@code terraform task}.
 *
 * @since 0.1
 */
@CompileStatic
class Variables implements VariablesSpec {

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

    /** Adds one variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param value Lazy-evaluated form of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.core.StringTools#stringize(Object)}
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
     *  Anything resolvable via {@link org.ysb33r.grolifant.api.core.StringTools#stringizeValues(Map)}
     * is accepted.
     */
    @Override
    void map(final String name, Map<String, ?> map) {
        varsFilesPair.vars.put(name, map)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.core.StringTools#stringize(Iterable <?>)} is accepted.
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
     * {@link org.ysb33r.grolifant.api.core.StringTools#stringize(Iterable <?>)} is accepted.
     */
    @Override
    void list(final String name, Iterable<?> vals) {
        varsFilesPair.vars.put(name, vals as List)
    }

    /** Adds a name of a file containing {@code terraform} variables.
     *
     * @param fileName Files that can be converted via
     * {@link org.ysb33r.grolifant.api.core.StringTools#stringize(Object o)} and resolved relative to the appropriate
     * {@link org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}.
     */
    @Override
    void file(final Object fileName) {
        varsFilesPair.files.add fileName
    }

    /** List of file names containing Terraform variables.
     *
     * Filenames can contain relative paths.
     *
     * @return List of filenames.
     */
    Set<String> getFileNames() {
        this.varsFilesPair.fileNames
    }

    List<String> getCommandLineArgs() {
        Path root = rootDirResolver.orNull?.toPath()
        if (root == null) {
            throw new TerraformConfigurationException(
                'This method can only be called when attached to a task extension or a source set'
            )
        }

        this.varsFilesPair.commandLineArgs(root)
    }

    @Override
    String toString() {
        "Terraform variables: ${this.varsFilesPair}"
    }

    /** Returns a description of the files and variables
     *
     * @return Files containing variables as well as explicitly declared variables.
     *
     * @since 0.2
     */
    VarsFilesPair getAllVars() {
        this.varsFilesPair
    }

    private final VarsFilesPair varsFilesPair = new VarsFilesPair()
    private final Provider<File> rootDirResolver
}
