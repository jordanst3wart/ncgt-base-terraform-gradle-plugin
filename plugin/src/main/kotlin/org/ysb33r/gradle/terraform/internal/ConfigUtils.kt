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

import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.errors.MissingConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.*
import java.util.Optional

/** Internal utilities for dealing with Terraform tool configuration.
 *
 * @author Schalk W. Cronjé
 */
object ConfigUtils {
    /** Locates the Terraform configuration file in use by the specific project
     *
     * @param terraformrc [TerraformRCExtension].
     * @return Location of Terraform config file. Never `null`.
     */
    fun locateTerraformConfigFile(terraformrc: TerraformRCExtension): File {
        return if (terraformrc.useGlobalConfig) {
            File(locateGlobalTerraformConfigAsString())
        } else {
            terraformrc.getTerraformRC().get()
        }
    }

    /** Locates the global `terraformrc file`
     *
     * @return Location of configuration file as a string.
     *
     * @throws [MissingConfiguration] if the location cannot be resolved.
     */
    fun locateGlobalTerraformConfigAsString(): String {
        val configFromEnv = System.getenv("TF_CLI_CONFIG_FILE")

        return if (configFromEnv != null && configFromEnv.isNotEmpty()) {
            configFromEnv
        } else {
            if (OperatingSystem.current().isWindows) {
                val appData = System.getenv("APPDATA")
                if (appData != null && appData.isNotEmpty()) {
                    "${appData}\\terraform.rc"
                } else {
                    throw MissingConfiguration("%APPDATA% not available")
                }
            } else {
                "${System.getProperty("user.home")}/.terraformrc"
            }
        }
    }

    /** Locates the [TerraformRCExtension] in the project
     *
     * Only call this method during configuration phase.
     *
     * @param project Project to start search from.
     * @return [TerraformRCExtension]. Never `null`.
     *
     * @throws [MissingConfiguration] if extension cannot be located.
     */
    @JvmStatic
    fun locateTerraformRCExtension(project: Project): TerraformRCExtension {
        var terraformrc: TerraformRCExtension
        try {
            try {
                terraformrc = project.extensions.getByType(TerraformRCExtension::class.java)
            } catch (e: UnknownDomainObjectException) {
                if (project != project.rootProject) {
                    terraformrc = project.rootProject.extensions.getByType(TerraformRCExtension::class.java)
                } else {
                    throw e
                }
            }
        } catch (e: UnknownDomainObjectException) {
            throw MissingConfiguration(
                "Cannot locate a TerraformRC Extension in this project or the root project",
                e
            )
        }
        return terraformrc
    }

    /** Creates the plugin cache directory if it is not a global configuration.
     *
     * @param terraformrc [TerraformRCExtension].
     * @return Location of the cache directory or `empty` if a global configuration is used.
     *
     * @throws [MissingConfiguration] if [TerraformRCExtension] cannot be located.
     *
     * @since 0.10.0
     */
    @JvmStatic
    fun createPluginCacheDir(terraformrc: TerraformRCExtension): Optional<File> {
        return if (terraformrc.useGlobalConfig) {
            Optional.empty()
        } else {
            val rc = terraformrc.getPluginCacheDir().get()
            rc.mkdirs()
            if (OperatingSystem.current().isUnix) {
                Files.setPosixFilePermissions(
                    rc.toPath(),
                    setOf(
                        OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                        GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                        OTHERS_READ, OTHERS_EXECUTE
                    )
                )
            }
            Optional.of(rc)
        }
    }
}