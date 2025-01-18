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

    void clear() {
        this.vars.clear()
        this.files.clear()
        this.additionalVariables.clear()
    }

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

    /**
     * Gets all variables in a Terraform file format.
     *
     * @return List of pairings.
     *
     * @since 0.13
     */
    List<String> getVarsInTfFormat() {
        getEscapedVars(true).collect { String k, String v ->
            "$k = $v".toString()
        }
    }

    /** Evaluate all provided and local variables and convert them to Terraform-compliant strings, ready to be
     * passed to command-line.
     *
     * Provided variables will be evaluated first, so that any local definitions can override them.
     *
     * <p> Calling this will resolve all lazy-evaluated entries.
     *
     * @param escapeInnerLevel Whether to escape inner level string. Default is {@code false}
     * @return Map where each key is the name of a variable. Each value is correctly formatted according to
     *   the kind of variable.
     *
     * @since 0.12
     */
    Map<String, String> getEscapedVars(boolean escapeInnerLevel = false) {
        Map<String, String> hclMap = escapeProvidedVars(escapeInnerLevel)
        hclMap.putAll(escapeLocalVars(escapeInnerLevel))
        hclMap
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

    private Map<String, String> escapeProvidedVars(boolean escapeInnerLevel) {
        def vars = new IntermediateVariableSpec(new VarsFilesPair())
        for (Action<VariablesSpec> additional : this.additionalVariables) {
            additional.execute(vars)
        }
        vars.vfp.escapeLocalVars(escapeInnerLevel)
    }

    private Map<String, String> escapeLocalVars(boolean escapeInnerLevel) {
        Map<String, String> hclMap = [:]
        for (String key in this.vars.keySet()) {
            hclMap[key] = escapeObject(this.vars[key], escapeInnerLevel)
        }
        hclMap
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

    private static class IntermediateVariableSpec implements VariablesSpec {
        final VarsFilesPair vfp

        IntermediateVariableSpec(VarsFilesPair vfp) {
            this.vfp = vfp
        }

        @Override
        void var(String name, Object value) {
            vfp.vars.put(name, value)
        }

        @Override
        void map(Map<String, ?> map, String name) {
            vfp.vars.put(name, map)
        }

        @Override
        void map(String name, Provider<Map<String, ?>> mapProvider) {
            vfp.vars.put(name, mapProvider)
        }

        @Override
        void list(String name, Object val1, Object... vals) {
            vfp.vars.put(name, [val1] + vals.toList())
        }

        @Override
        void list(String name, Iterable<?> vals) {
            vfp.vars.put(name, vals)
        }

        @Override
        void file(Object fileName) {
            vfp.files.add(fileName)
        }

        @Override
        void provider(Action<VariablesSpec> additionalVariables) {
            vfp.additionalVariables.add(additionalVariables)
        }
    }
}

