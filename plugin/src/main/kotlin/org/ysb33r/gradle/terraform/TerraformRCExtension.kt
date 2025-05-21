package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.internal.os.OperatingSystem
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
        const val TERRAFORM_RC_TASK = "setupTerraform"
    }

    var disableCheckPoint = true
    var disableCheckPointSignature = false
    var pluginCacheMayBreakDependencyLockFile = false

    val pluginCacheDir: DirectoryProperty = project.objects.directoryProperty()
    val terraformRC: RegularFile = project.layout.projectDirectory.file(".gradle/.terraformrc")

    init {
        pluginCacheDir.set(File(project.gradle.gradleUserHomeDir, "caches/terraform.d"))
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

    // should move to a task
    fun createPluginCacheDir(): DirectoryProperty {
        val rc = this.pluginCacheDir.get().asFile
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
        return this.pluginCacheDir
    }

    fun locateTerraformConfigFile(): File {
        return this.terraformRC.asFile
    }
}