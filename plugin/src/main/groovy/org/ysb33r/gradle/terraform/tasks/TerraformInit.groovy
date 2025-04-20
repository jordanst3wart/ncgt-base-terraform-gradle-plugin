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
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.errors.MissingTerraformConfiguration

import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime

import static java.nio.file.FileVisitResult.CONTINUE
import static java.nio.file.Files.readSymbolicLink

/** Equivalent of {@code terraform init}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformInit extends AbstractTerraformTask {

    /**
     * Skip initialisation of child modules.
     */
    @Internal
    boolean skipChildModules = false

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
        super('init', [])
        supportsInputs()
        supportsColor()

        this.backendConfig = project.objects.property(File)
        // TODO I think this is wrong it should use the cached plugins...
        // might not need the second map
        this.pluginDirectory = sourceSet.map { sourceSet ->
            sourceSet.dataDir.map { new File(it, PLUGIN_SUBDIR) } } as Provider<File>
        this.terraformStateFile = sourceSet.map { sourceSet ->
            sourceSet.dataDir.map { new File(it, 'terraform.tfstate') } } as Provider<File>
        this.terraformInitStateFile = sourceSet.map { sourceSet ->
            sourceSet.dataDir.map { new File(it as File, '.init.txt') } } as Provider<File>
        this.useBackendConfig = project.objects.property(Boolean)
    }

    /** Configuration for Terraform backend.
     *
     * See {@link https://www.terraform.io/docs/backends/config.html#partial-configuration}
     *
     * @return Location of configuration file. Can be {@code null} if none is required.
     *
     * @since 0.4.0
     */
    @Optional
    @InputFile
    Property<File> getBackendConfigFile() {
        this.backendConfig
    }

    /** Set location of backend configuration file.
     *
     * @param location Anything that can be converted using {@code project.file}.
     *
     * @since 0.4.0
     */
    void setBackendConfigFile(File backendFile) {
        backendConfig.set(backendFile)
    }

    void setUseBackendFile(Provider<Boolean> useBackend) {
        this.useBackendConfig = useBackend
    }

    @Input
    Provider<Boolean> getUseBackendFile() {
        this.useBackendConfig
    }

    @Override
    void exec() {
        removeDanglingSymlinks()
        super.exec()
        terraformInitStateFile.get().text = "${LocalDateTime.now()}"
    }

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

        execSpec.cmdArgs "-get=${!skipChildModules}"

        if (!this.backendConfig.get().exists()) {
            throw new MissingTerraformConfiguration("cannot location ${this.backendConfig.get().absolutePath}")
        }

        if (this.useBackendConfig.get()) {
            execSpec.cmdArgs("-backend-config=${this.backendConfig.get().absolutePath}")
        }

        execSpec
    }

    private void removeDanglingSymlinks() {
        // TODO check this function... I don't know what it does
        Path pluginDir = new File(sourceSet.get().dataDir.get(), PLUGIN_SUBDIR).toPath()
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

    private Provider<Boolean> useBackendConfig
    private final Property<File> backendConfig
    private static final String PLUGIN_SUBDIR = 'plugins'
}
