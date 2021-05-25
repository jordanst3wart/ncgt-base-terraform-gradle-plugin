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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.grolifant.api.core.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations

/** General utilities for Terraform.
 *
 * @since 0.2
 */
@CompileStatic
class TerraformUtils {

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
        String path = projectOperations.file(file).absolutePath
        OperatingSystem.current().windows ? path.replaceAll(~/\x5C/, FORWARD_SLASH) : path
    }

    /** Get all AWS-related environmental variables.
     *
     * @return Map of environmental variables.
     */
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
     * @since 0.9.0
     *
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
     * @param name Name of the source set
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
            TF_DATA_DIR       : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE: TerraformConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
            TF_LOG_PATH       : new File(logDir.get(), "${name}.log").absolutePath,
            TF_LOG            : logLevel ?: '',
        ]
    }

    private static final String FORWARD_SLASH = '/'
}
