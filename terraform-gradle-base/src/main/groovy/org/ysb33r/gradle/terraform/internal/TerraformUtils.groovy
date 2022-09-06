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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.grolifant.api.core.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.StringUtils

/** General utilities for Terraform.
 *
 * @since 0.2
 */
@CompileStatic
class TerraformUtils {

    static public final String PLUGIN_VERSION

    static {
        def props = new Properties()
        TerraformUtils.getResourceAsStream('/terraform-gradle-templates/terraform.properties')
            .withCloseable { input ->
                props.load(input)
            }
        PLUGIN_VERSION = props['plugin-version']
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param project Project context.
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     * @deprecated
     */
    @Deprecated
    static String terraformPath(Project project, Object file) {
        String path = project.file(file).absolutePath
        OperatingSystem.current().windows ? path.replaceAll(~/\x5C/, FORWARD_SLASH) : path
    }

    /** Converts a file path to a format suitable for interpretation by Terraform on the appropriate
     * platform.
     *
     * @param projectOperations Project context.
     * @param file Object that can be converted using {@code project.file}.
     * @return String version adapted on a per-platform basis
     */
    static String terraformPath(ProjectOperations projectOperations, Object file) {
        String path = projectOperations.fsOperations.file(file).absolutePath
        OperatingSystem.current().windows ? path.replaceAll(~/\x5C/, FORWARD_SLASH) : path
    }

    /** Get all AWS-related environmental variables.
     *
     * @return Map of environmental variables.
     * @deprecated
     */
    @Deprecated
    static Map<String, String> awsEnvironment() {
        System.getenv().findAll { k, v -> k.startsWith('AWS_') }
    }

    /** Obtain the required terraform execution environmental variables
     *
     * @param project Project context
     * @param name Name of the source set
     * @param dataDir Data directory provider
     * @param logDir Log directory provider
     * @param logLevel Level of logging. Can be {@code null}.
     * @return Map of environmental variables
     *
     * @since 0.9.0*
     * @deprecated
     */
    @Deprecated
    static Map terraformEnvironment(
        Project project,
        String name,
        Provider<File> dataDir,
        Provider<File> logDir,
        String logLevel
    ) {
        [
            TF_DATA_DIR       : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE: TerraformConfigUtils.locateTerraformConfigFile(project).absolutePath,
            TF_LOG_PATH       : new File(logDir.get(), "${name}.log").absolutePath,
            TF_LOG            : logLevel ?: '',
        ]
    }

    /** Obtain the required terraform execution environmental variables
     *
     * @param terraformrc {@link TerraformRCExtension}.
     * @param name Name of the task
     * @param dataDir Data directory provider
     * @param logDir Log directory provider
     * @param logLevel Level of logging. Can be {@code null}.
     * @return Map of environmental variables
     *
     * @since 0.10.0
     */
    static Map terraformEnvironment(
        TerraformRCExtension terraformrc,
        String name,
        Provider<File> dataDir,
        Provider<File> logDir,
        String logLevel
    ) {
        [
            TF_DATA_DIR         : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE  : TerraformConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
            TF_LOG_PATH         : terraformLogFile(name, logDir).absolutePath,
            TF_LOG              : logLevel ?: '',
            TF_APPEND_USER_AGENT: "terraform-gradle-plugin/${PLUGIN_VERSION}"
        ]
    }

    /**
     * Resolves the location of the log file.
     *
     * @param name Task name
     * @param logDir Log dir provider
     * @return Location of log file
     *
     * @since 0.11
     */
    static File terraformLogFile(String name, Provider<File> logDir) {
        new File(logDir.get(), "${name}.log").absoluteFile
    }

    /**
     * Escape HCL variables in a form suitable for using in a variables or backend configuration file.
     *
     * @param vars Variables map to escape
     * @param escapeInnerLevel Whether inner level string variables should be escaped.
     * @return Escaped map.
     *
     * @sinec 0.13
     */
    static Map<String, String> escapeHclVars(Map<String, Object> vars, boolean escapeInnerLevel) {
        Map<String, String> hclMap = [:]
        for (String key in vars.keySet()) {
            hclMap[key] = escapeOneItem(vars[key], escapeInnerLevel)
        }
        hclMap
    }

    /**
     * Takes a list and creates a HCL-list with appropriate escaping.
     *
     * @param items List items
     * @return Escaped string
     *
     * @since 0.12
     */
    static String escapedList(Iterable<Object> items, boolean escapeInnerLevel) {
        String joinedList = Transform.toList(items as Collection) { Object it ->
            escapeOneItem(it, escapeInnerLevel)
        }.join(COMMA_SEPARATED)
        "[${joinedList}]"
    }

    /**
     * Takes a map and creates a HCL-map with appropriate escaping.
     *
     * @param items Map items
     * @return Escaped string
     *
     * @since 0.12
     */
    static String escapedMap(Map<String, ?> items, boolean escapeInnerLevel) {
        String joinedMap = Transform.toList(items) { Map.Entry<String, ?> entry ->
            "\"${entry.key}\" = ${escapeOneItem(entry.value, escapeInnerLevel)}".toString()
        }.join(COMMA_SEPARATED)
        "{${joinedMap}}".toString()
    }

    /**
     * Escaped a single item.
     *
     * @param item Item to escape
     * @param innerLevel Whether the escaped item is actually nested.
     * @return Escaped item
     *
     * @since 0.12
     */
    static String escapeOneItem(Object item, boolean innerLevel) {
        switch (item) {
            case Provider:
                return escapeOneItem(((Provider) item).get(), innerLevel)
            case Map:
                return escapedMap((Map) item, innerLevel)
            case Iterable:
                return escapedList((Iterable) item, innerLevel)
            case Number:
            case Boolean:
                return StringUtils.stringize(item)
            default:
                return innerLevel ?
                    "\"${escapeQuotesInString(StringUtils.stringize(item))}\"".toString() :
                    escapeQuotesInString(StringUtils.stringize(item))
        }
    }

    /**
     * Escapes any Terraform string quotes.
     *
     * @param item String to escape
     * @return Escape string.
     */
    static String escapeQuotesInString(String item) {
        item.replaceAll(~/"/, '\\\\"')
    }

    /**
     * Converts item to string if not null
     *
     * @param thingy Item that needs to be converted to a string
     * @return Stringized item or {@code null}.
     *
     * @since 1.0
     */
    static String stringizeOrNull(Object thingy) {
        thingy != null ? StringUtils.stringize(thingy) : null
    }

    private static final String FORWARD_SLASH = '/'
    private static final String COMMA_SEPARATED = ', '
}
