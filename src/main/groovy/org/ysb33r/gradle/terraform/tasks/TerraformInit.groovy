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
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.grolifant.api.v4.MapUtils

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime

import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.Files.readSymbolicLink

/** Equivalent of {@code terraform init}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformInit extends AbstractTerraformTask {

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

    /** Whether backend configuration should be skipped.
     *
     * This option can be set from the command-line with {@code --no-configure-backends}
     *
     * @since 0.6.0
     */
    @Option(option = 'no-configure-backends', description = 'Skip backend configuration')
    @Internal
    boolean skipConfigureBackends = false

    @Option(option = 'force-copy', description = 'Automatically answer yes to any backend migration questions')
    @Internal
    boolean forceCopy = false

    @Option(option = 'reconfigure',
        description = 'Disregard any existing configuration and prevent migration of existing state')
    @Internal
    boolean reconfigure = false

    /**
     * The directory where terraform plgins data is written to.
     *
     * @since 0.10
     */
    @OutputDirectory
    final Provider<File> pluginDirectory

    /**
     * The location of {@code terraform.tfstate}.
     *
     * @since 0.10
     */
    @OutputFile
    final Provider<File> terraformStateFile

    /**
     * The location of the file that provides details about the last run of this task.
     */
    @OutputFile
    final Provider<File> terraformInitStateFile

    TerraformInit() {
        super('init', [Lock], [])
        supportsInputs()
        supportsColor()

        this.backendConfig = project.objects.property(File)
        this.pluginDirectory = dataDir.map( {new File(it,'plugins')})
        this.terraformStateFile = dataDir.map( {new File(it,'terraform.tfstate')})
        this.terraformInitStateFile = dataDir.map( {new File(it,'.init.txt')})
    }

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
        projectOperations.updateFileProperty(this.backendConfig, location)
    }

    /** Backend configuration files.
     *
     * This can be set in addition or as alternative to using a configuration file for the backend.
     *
     * @return Map of configuration values. Never {@code null}.
     *
     * @since 0.4.0
     */
    @Input
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

    @Override
    void exec() {
        removeDanglingSymlinks()
        super.exec()
        terraformInitStateFile.get().text = "${LocalDateTime.now()}"
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

        if (projectOperations.offline) {
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

        execSpec.cmdArgs "-backend=${!skipConfigureBackends}"
        execSpec.cmdArgs "-verify-plugins=${verifyPlugins}"

        getBackendConfigValues().each { String k, String v ->
            execSpec.cmdArgs "-backend-config=$k=$v"
        }

        if (this.backendConfig.present) {
            execSpec.cmdArgs("-backend-config=${this.backendConfig.get().absolutePath}")
        }

        if (this.forceCopy) {
            execSpec.cmdArgs('-force-copy')
        }

        if (this.reconfigure) {
            execSpec.cmdArgs('-reconfigure')
        }

        execSpec
    }

    private void removeDanglingSymlinks() {
        Path pluginDir = new File(dataDir.get(), 'plugins').toPath()
        Files.walkFileTree(
            pluginDir,
            new FileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    CONTINUE
                }

                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.symbolicLink && !Files.exists(readSymbolicLink(file))) {
                        logger.debug("Removing dangling plugin symbolic link ${file}")
                        Files.delete(file)
                    }
                    CONTINUE
                }

                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    logger.debug("Failed to visit: ${file}, because ${exc.message}")
                    CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    CONTINUE
                }
            }
        )
    }

    private final Property<File> backendConfig
    private final Map<String, Object> backendConfigValues = [:]
}
