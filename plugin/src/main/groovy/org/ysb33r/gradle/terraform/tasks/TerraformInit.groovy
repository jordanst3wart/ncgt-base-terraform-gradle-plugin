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
import static org.ysb33r.gradle.terraform.internal.TerraformConfigUtils.createPluginCacheDir

/** Equivalent of {@code terraform init}.
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
     */
    @OutputDirectory
    Provider<File> getPluginDirectory() {
        sourceSet.map { source ->
            source.dataDir.map { new File(it as File, 'providers') } } as Provider<File>
        // new File(sourceSet.get().dataDir.get(), "${sourceSet.get().name}.tf.plan")
    }

    // TODO not having these assumes you are using a remote backend...
    /**
     * The location of {@code terraform.tfstate}.
     */
    //@OutputFile
    //final Provider<File> terraformStateFile

    /**
     * The location of the file that provides details about the last run of this task.
     */
    //@OutputFile
    //final Provider<File> terraformInitStateFile

    TerraformInit() {
        super('init', [])
        supportsInputs()
        supportsColor()
        this.backendConfig = project.objects.property(File)
        this.useBackendConfig = project.objects.property(Boolean)
    }

    /** Configuration for Terraform backend.
     *
     * See {@link https://www.terraform.io/docs/backends/config.html#partial-configuration}
     *
     * @return Location of configuration file. Can be {@code null} if none is required.
     */
    @Optional
    @InputFile
    Property<File> getBackendConfigFile() {
        this.backendConfig
    }

    /** Set location of backend configuration file.
     *
     * @param location Anything that can be converted using {@code project.file}.
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
        createPluginCacheDir(this.terraformrc)
        super.exec()
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

    private Provider<Boolean> useBackendConfig
    private final Property<File> backendConfig
}
