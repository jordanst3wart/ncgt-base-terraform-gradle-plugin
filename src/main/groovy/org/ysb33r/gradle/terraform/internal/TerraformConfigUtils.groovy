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
package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.UnknownTaskException
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.errors.MissingTerraformConfiguration
import org.ysb33r.grolifant.api.OperatingSystem

import java.nio.file.Files

@SuppressWarnings('NoWildcardImports')
import static java.nio.file.attribute.PosixFilePermission.*
import static org.ysb33r.gradle.terraform.plugins.TerraformRCPlugin.TERRAFORM_RC_TASK

/** Internal utilities for dealing with Terraform tool configuration.
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformConfigUtils {

    /** Locates the Terraform configuration file in use by the specific project
     *
     * @param project Project requesting Terraform location.
     * @return Location of Terraform config file. Never {@code null}.
     */
    static File locateTerraformConfigFile(Project project) {
        TerraformRCExtension ext = locateTerraformRCExtension(project)

        ext.useGlobalConfig ? new File(locateGlobalTerraformConfigAsString()) : ext.terraformRC.get()
    }

    /** Locates the global {@code terraformrc file}
     *
     * @return Location of configuration file as a string.
     *
     * @throw {@link MissingTerraformConfiguration} if the location cannot be resolved.
     */
    static String locateGlobalTerraformConfigAsString() {
        String configFromEnv = System.getenv('TF_CLI_CONFIG_FILE')

        if (configFromEnv) {
            configFromEnv
        } else {
            if (OperatingSystem.current().windows) {
                String appData = System.getenv('APPDATA')
                if (appData) {
                    "${appData}\\terraform.rc"
                } else {
                    throw new MissingTerraformConfiguration('%APPDATA% not available')
                }
            } else {
                "${System.getProperty('user.home')}/.terraformrc"
            }
        }
    }

    /** Locates the {@link TerraformRCExtension} in the project
     *
     * @param project Project to start search from.
     * @return {@link TerraformRCExtension}. Never {@code null}.
     *
     * @throw {@link MissingTerraformConfiguration} if extension cannot be located.
     */
    static TerraformRCExtension locateTerraformRCExtension(Project project) {
        TerraformRCExtension terraformrc
        try {
            try {
                terraformrc = project.extensions.getByType(TerraformRCExtension)
            } catch (UnknownDomainObjectException e) {
                if (project != project.rootProject) {
                    terraformrc = project.rootProject.extensions.getByType(TerraformRCExtension)
                } else {
                    throw e
                }
            }
        } catch (UnknownDomainObjectException e) {
            throw new MissingTerraformConfiguration(
                'Cannot locate a TerraformRC Extension in this project or the root project',
                e
            )
        }
        terraformrc
    }

    /** Locates the task that can generate a {@code terraformrc} file.
     *
     * @param project Project to start search from.
     * @return Task.*
     * @throw {@link MissingTerraformConfiguration} if task cannot be located.
     */
    static Task locateTerraformRCGenerator(Project project) {
        TerraformRCExtension ext = locateTerraformRCExtension(project)

        try {
            ext.project.tasks.getByName(TERRAFORM_RC_TASK)
        } catch (UnknownTaskException e) {
            throw new MissingTerraformConfiguration(
                'Cannot locate a task in this project or the root project which could generate the terraformrc file',
                e
            )
        }
    }

    /** Creates the plugin cache directory if it is not a global configuration.
     *
     * @param project Project from which this is called.
     * @return Location of the cache directory or {@code empty} if a global configuration is used.
     *
     * @throw {@link MissingTerraformConfiguration} if {@link TerraformRCExtension} cannot be located.
     */
    static Optional<File> createPluginCacheDir(Project project) {
        TerraformRCExtension terraformrc = locateTerraformRCExtension(project)
        if (terraformrc.useGlobalConfig) {
            Optional.empty()
        } else {
            File rc = terraformrc.pluginCacheDir.get()
            rc.mkdirs()
            Files.setPosixFilePermissions(rc.toPath(), [
                OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                OTHERS_READ, OTHERS_EXECUTE
            ].toSet())
            Optional.of(rc)
        }
    }
}
