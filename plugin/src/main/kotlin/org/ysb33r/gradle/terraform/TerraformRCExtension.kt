package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.errors.MissingConfiguration
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath
import java.io.File
import java.io.Writer
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
import java.nio.file.attribute.PosixFilePermission.GROUP_READ
import java.nio.file.attribute.PosixFilePermission.GROUP_WRITE
import java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OTHERS_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE

/** Extension that describes a `terraformrc` file.
 * find details about options here: https://developer.hashicorp.com/terraform/cli/config/config-file
 */
open class TerraformRCExtension(project: Project) {
    companion object {
        const val TERRAFORM_RC_TASK = "generateTerraformConfig"

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
    }

    var disableCheckPoint = true
    var disableCheckPointSignature = false
    var useGlobalConfig = false
    var pluginCacheMayBreakDependencyLockFile = false

    private val pluginCacheDir: DirectoryProperty = project.objects.directoryProperty()
    private val terraformRC: RegularFile = project.layout.projectDirectory.file(".gradle/.terraformrc")

    init {
        pluginCacheDir.set(File(project.gradle.gradleUserHomeDir, "caches/terraform.d"))
    }

    fun setPluginCacheDir(dir: File) {
        this.pluginCacheDir.set(dir)
    }

    fun getPluginCacheDir(): DirectoryProperty {
        return this.pluginCacheDir
    }

    fun getTerraformRC(): RegularFile {
        return this.terraformRC
    }

    /** Writes to the content of the Terraform configuration to an HCL writer.
     *
     * @param writer Writer instance to send output to.
     * @return The writer
     */
    fun toHCL(writer: Writer): Writer {
        writer.write("disable_checkpoint = ${this.disableCheckPoint}\n")
        writer.write("disable_checkpoint_signature = ${this.disableCheckPointSignature}\n")
        writer.write("plugin_cache_dir = \"${escapedFilePath(OperatingSystem.current(), pluginCacheDir.get().asFile)}\"\n")
        writer.write("plugin_cache_may_break_dependency_lock_file = ${this.pluginCacheMayBreakDependencyLockFile}\n")
        return writer
    }

    fun createPluginCacheDir() {
        if (this.useGlobalConfig) {
            return
        }

        val rc = this.getPluginCacheDir().get().asFile
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
    }

    fun locateTerraformConfigFile(): File {
        return if (this.useGlobalConfig) {
            File(locateGlobalTerraformConfigAsString())
        } else {
            this.getTerraformRC().asFile
        }
    }

    private fun locateGlobalTerraformConfigAsString(): String {
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
}