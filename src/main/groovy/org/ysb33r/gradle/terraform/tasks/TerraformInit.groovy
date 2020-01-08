/*
 * Copyright 2017-2020 the original author or authors.
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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.grolifant.api.MapUtils

import java.util.concurrent.Callable

/** Equivalent of {@code terraform init}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformInit extends AbstractTerraformTask {

    TerraformInit() {
        super('init', [Lock], [])
        supportsInputs()
        supportsColor()
    }

    // TODO: Implement -from-module=MODULE-SOURCE as Gradle @Option

    /** Whether modules should be upgraded
     *
     * This option can be set from the command-line with {@code --upgrade=true}.
     */
    @Option(option = 'upgrade', description = 'Force upgrade of modules and plugins when not offline')
    @Internal
    boolean upgrade = false

    /**
     * Skip initialisation of child modules.
     */
    @Internal
    boolean skipChildModules = false

    /** Whether backends should be configured.
     *
     * This option can be set from the command-line with {@code --configure-backends=true}
     */
    @Option(option = 'configure-backends', description = 'Whether backends should be configured')
    @Internal
    boolean configureBackend = true

    // TODO: Other backend settings
    // -force-copy
    // -reconfigure

    /** Configuration for Terraform backend.
     *
     * See {@link https://www.terraform.io/docs/backends/config.html#partial-configuration}
     *
     * @return Location of configuration file. Can be {@code null} if none is required.
     *
     * @since 0.4.0
     */
    @InputFile
    @Optional
    Provider<File> getBackendConfigFile() {
        this.backendConfig
    }

    /** Set location of backend configuration file.
     *
     * @param location Anything that can be converted using {@code project.file}.
     *
     * @since 0.4.0
     */
    void setBackendConfigFile(Object location) {
        this.backendConfig = location ? project.provider({ ->
            project.file(location)
        } as Callable<File>) : null
    }

    /** Backend configuration files.
     *
     * This can be set in addition or as alternative to using a configuration file for the backend.
     *
     * @return Map of configuration values. Never {@code null}.
     *
     * @since 0.4.0
     */
    Map<String, String> getBackendConfigValues() {
        MapUtils.stringizeValues(this.backendConfigValues)
    }

    /** Replaces any existing backend configuration values with a new set.
     *
     * It does not affect anything specified via a configuration file.
     *
     * @param backendValues Map of replacement key-value pairs.
     *
     * @since 0.4.0
     */
    void setBackendConfigValues(Map<String, Object> backendValues) {
        this.backendConfigValues.clear()
        this.backendConfigValues.putAll(backendValues)
    }

    /** Adds additional backend configuration values
     *
     * @param backendValues Map of key-value pairs.
     *
     * @since 0.4.0
     */
    void backendConfigValues(Map<String, Object> backendValues) {
        this.backendConfigValues.putAll(backendValues)
    }

    /** Adds a single backend value.
     *
     * @param key Name of backend configuration
     * @param value Value of backend configuration.
     *
     * @since 0.4.0
     */
    void backendConfigValue(String key, Object value) {
        this.backendConfigValues.put(key, value)
    }

    /** Whether plugins should be verified.
     *
     */
    @Internal
    boolean verifyPlugins = true

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (project.gradle.startParameter.offline) {
            logger.warn(
                'Gradle is running in offline mode. ' +
                    (upgrade ? 'Upgrade will not be attempted. ' : '') +
                    (skipChildModules ? '' : 'Modules will not be retrieved. ')
            )
            execSpec.cmdArgs '-get=false'
        } else {
            if (upgrade) {
                execSpec.cmdArgs('-upgrade')
            }
            execSpec.cmdArgs "-get=${!skipChildModules}"
        }

        execSpec.cmdArgs "-backend=${configureBackend}"
        execSpec.cmdArgs "-verify-plugins=${verifyPlugins}"

        getBackendConfigValues().each { String k, String v ->
            execSpec.cmdArgs "-backend-config=\"$k=$v\""
        }

        if (this.backendConfig) {
            execSpec.cmdArgs("-backend-config=${this.backendConfig.get().absolutePath}")
        }

        execSpec
    }

//    private boolean checkVariables = true
    private Provider<File> backendConfig
    private final Map<String, Object> backendConfigValues = [:]
}
