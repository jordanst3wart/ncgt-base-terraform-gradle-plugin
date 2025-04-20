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
import org.gradle.api.Action
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.config.VariablesSpec
import org.ysb33r.grashicorp.StringUtils

import java.nio.file.Path
import java.util.stream.Collectors

import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapeOneItem
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedList
import static org.ysb33r.gradle.terraform.internal.TerraformUtils.escapedMap

/** Keeps a collection of variables and variable files.
 *
 * @since 0.2
 */
@CompileStatic
class VarsFilesPair {
    /**
     * Terraform variables.
     *
     * Key is the name of the variable.
     *
     * Value can be a map, collection, boolean, number or anything convertible to a string.
     */
    final Map<String, Object> vars = [:]

    /**
     * List of filenames or relative paths to a terraform source set.
     */
    final List<Object> files = []

    /**
     * Additional variables from other sources.
     *
     * @since 0.12
     */
    final List<Action<VariablesSpec>> additionalVariables = []

    /**
     * Copy these settings to another instance.
     *
     * @param target Target instance
     *
     * @since 0.12
     */
    void copyTo(VarsFilesPair target) {
        target.files.addAll(this.files)
        target.vars.putAll(this.vars)
        target.additionalVariables.addAll(this.additionalVariables)
    }

    /**
     * Returns the command-line representation.
     *
     * @param root Root path for resolving file paths.
     * @return A list of terraform command-line arguments related to variables and file-based variables.
     *
     * @since 0.12
     */
    List<String> commandLineArgs(Path root) {
        final List<String> varList = []
        varList.addAll(fileNames.stream().map { String fileName ->
            "-var-file=${root.resolve(fileName).toFile().absolutePath}".toString()
        }.collect(Collectors.toList()) as List<String>)
        varList
    }

    /** List of file names containing Terraform variables.
     *
     * Filenames can contain relative paths.
     *
     * @return List of filenames.
     *
     * @since 0.12
     */
    Set<String> getFileNames() {
        StringUtils.stringize(this.files).toSet()
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    String toString() {
        "vars=${vars}, files=${files}, additionalsCount=${additionalVariables.size()}"
    }

    private String escapeObject(Object variable, boolean escapeInnerLevel) {
        switch (variable) {
            case Provider:
                return escapeObject(((Provider) variable).get(), escapeInnerLevel)
            case Map:
                return escapedMap((Map) variable, escapeInnerLevel)
            case List:
                return escapedList((Iterable) variable, escapeInnerLevel)
            default:
                return escapeOneItem(variable, escapeInnerLevel)
        }
    }
}

