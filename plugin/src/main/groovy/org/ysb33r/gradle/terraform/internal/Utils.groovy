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
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.grashicorp.StringUtils
import static org.ysb33r.gradle.terraform.internal.Downloader.OS

/** General utilities for Terraform.
 *
 */
@CompileStatic
class TerraformUtils {

    /** Get all AWS-related environmental variables.
     *
     * @return Map of environmental variables.
     */
    static Map<String, String> awsEnvironment() {
        System.getenv().findAll { k, v -> k.startsWith('AWS_') }
    }

    /** Get all Google-related environmental variables.
     *
     * @return Map of environmental variables.
     */
    static Map<String, String> googleEnvironment() {
        System.getenv().findAll { k, v -> k.startsWith('GOOGLE_') }
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
    static Map<String, String> terraformEnvironment(
        TerraformRCExtension terraformrc,
        String name,
        Provider<File> dataDir,
        Provider<File> logDir,
        String logLevel
    ) {
        def environment = [
            TF_DATA_DIR         : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE  : TerraformConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
            TF_LOG_PATH         : terraformLogFile(name, logDir).absolutePath,
            TF_LOG              : logLevel ?: '',
        ]
        environment.putAll(defaultEnvironment())
        environment
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

    static String escapeQuotesInString(String item) {
        item.replaceAll(~/"/, '\\\\"')
    }

    static String stringizeOrNull(Object thingy) {
        thingy != null ? StringUtils.stringize(thingy) : null
    }

    private static Map<String, String> defaultEnvironment() {
        if (OS.windows) {
            [
                TEMP        : System.getenv('TEMP'),
                TMP         : System.getenv('TMP'),
                HOMEDRIVE   : System.getenv('HOMEDRIVE'),
                HOMEPATH    : System.getenv('HOMEPATH'),
                USERPROFILE : System.getenv('USERPROFILE'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ]
        } else {
            [
                HOME        : System.getProperty('user.home'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ]
        }
    }

    private static final String COMMA_SEPARATED = ', '
}
