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
package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.tasks.AbstractTerraformTask
import org.ysb33r.grolifant.api.OperatingSystem
import org.ysb33r.grolifant.api.StringUtils

import java.nio.file.Path
import java.util.stream.Collectors

import static org.ysb33r.grolifant.api.MapUtils.stringizeValues

/** A configuration building block for tasks that need to pass variables to
 * a {@code terraform task}.
 *
 * >p> To add this to a task
 *
 * <code>
 * void variables(@DelegatesTo(Variables) Closure cfg) {*   configureNested(this.variables,cfg)
 *}*
 * void variables(Action<Variables> a) {*   a.execute(this.variables)
 *}*
 * // At construction time, initialise this with a reference to
 * // the task it is attached to.
 * @Nested
 * final Variables variables
 * </code>
 *
 * @since 0.1
 */
@CompileStatic
class Variables implements TerraformTaskConfigExtension {

    final String name = 'variables'

    /** Attached this configuration block to a task at construction time.
     *
     * @param task Associated task
     */
    Variables(AbstractTerraformTask task) {
        this.terraformTask = task
    }

    /** Adds one variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param value Lazy-evaluated form of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Object)}
     * is accepted.
     */
    void var(final String name, final Object value) {
        vars.put(name, value)
    }

    /** Adds a map as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable.
     *  Anything resolvable via {@link org.ysb33r.grolifant.api.MapUtils#stringizeValues(Map < String, Object >)}
     * is accepted.
     */
    void map(Map<String, ?> map, final String name) {
        vars.put(name, map)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param val1 First
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    void list(final String name, Object val1, Object... vals) {
        List<Object> inputs = [val1]
        inputs.addAll(vals)
        vars.put(name, inputs)
    }

    /** Adds a list as a variable.
     *
     * <p> This will replace any previous entry by the same name.
     *
     * @param name Name of variable.
     * @param vals Lazy-evaluated forms of variable. Anything resolvable via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Iterable <?>)} is accepted.
     */
    void list(final String name, Iterable<?> vals) {
        vars.put(name, vals as List)
    }

    /** Adds a name of a file containing {@code terraform} variables.
     *
     * @param fileName Files that can be converted via
     * {@link org.ysb33r.grolifant.api.StringUtils#stringize(Object o)} and resolved relative to the appropriate
     * {@link org.ysb33r.gradle.terraform.TerraformSourceDirectorySet}.
     */
    void file(final Object fileName) {
        files.add fileName
    }

    /** Evaluate all variables and convert them to Terraform-compliant strings, ready to be passed to command-line.
     *
     * <p> Calling this will resolve all lazy-evaluated entries.
     *
     * @return Map where each key is the name of a variable. Each value is correctly formatted according to
     *   the kind of variable.
     */
    @SuppressWarnings('DuplicateStringLiteral')
    Map<String, String> getEscapedVars() {
        Map<String, String> hclMap = [:]
        for (String key in this.vars.keySet()) {
            Object var = vars[key]
            switch (var) {
                case Map:
                    String joinedMap = stringizeValues((Map) var).collect { String k, String v ->
                        "\"${k}\" : \"${v}\"".toString()
                    }.join(', ')
                    hclMap[key] = "{${joinedMap}}".toString()
                    break
                case List:
                    String joinedList = StringUtils.stringize((Iterable) var).collect {
                        "\"${it}\"".toString()
                    }.join(', ')
                    hclMap[key] = "[${joinedList}]".toString()
                    break
                default:
                    hclMap[key] = StringUtils.stringize(var)
            }
        }
        hclMap
    }

    /** List of file names containing Terraform variables.
     *
     * Filenames can contain relative paths.
     *
     * @return List of filenames.
     */
    Set<String> getFileNames() {
        StringUtils.stringize(this.files).toSet()
    }

    @Override
    @SuppressWarnings('UnnecessaryCast')
    List<Closure> getInputProperties() {
        [
            { Map m ->
                stringizeValues(m)
            }.curry(this.vars),
            { ->
                fileNames
            }
        ] as List<Closure>
    }

    @Override
    List<String> getCommandLineArgs() {
        Path root = terraformTask.sourceDir.get().toPath()
        final List<String> varList = escapedVars.collectMany { String k, String v ->
            ['-var', "$k=$v".toString()]
        } as List<String>
        varList.addAll(fileNames.stream().map { String fileName ->
            "-var-file=${root.resolve(fileName).toFile().absolutePath}".toString()
        }.collect(Collectors.toList()))
        varList
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    String terraformPath(Object file) {
        String path = terraformTask.project.file(file).absolutePath
        OperatingSystem.current().windows ? path.replaceAll(~/\x5C/, '/') : path
    }

    private final AbstractTerraformTask terraformTask
    private final Map<String, Object> vars = [:]
    private final List<Object> files = []
}
