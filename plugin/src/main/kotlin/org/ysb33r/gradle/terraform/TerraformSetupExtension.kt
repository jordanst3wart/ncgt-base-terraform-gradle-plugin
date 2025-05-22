package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.ysb33r.gradle.terraform.TerraformExtension.Companion.TERRAFORM_DEFAULT
import org.ysb33r.gradle.terraform.internal.Executable
import java.io.File

/** Extension that describes a `terraformrc` file, cache directory, and downloads native binary.
 * find details about options here: https://developer.hashicorp.com/terraform/cli/config/config-file
 */
open class TerraformSetupExtension(project: Project) {
    companion object {
        const val TERRAFORM_SETUP_TASK = "setupTerraform"
    }

    val disableCheckPoint = project.objects.property(Boolean::class.java)
    val disableCheckPointSignature = project.objects.property(Boolean::class.java)
    val pluginCacheMayBreakDependencyLockFile = project.objects.property(Boolean::class.java)
    val executable = project.objects.property(Executable::class.java)
    val executableVersion = project.objects.property(String::class.java)

    val pluginCacheDir: DirectoryProperty = project.objects.directoryProperty()
    val terraformRC: RegularFile = project.layout.projectDirectory.file(".gradle/.terraformrc")

    init {
        pluginCacheDir.set(File(project.gradle.gradleUserHomeDir, "caches/terraform.d"))
        disableCheckPoint.set(true)
        disableCheckPointSignature.set(false)
        pluginCacheMayBreakDependencyLockFile.set(false)
        executable.set(Executable.TERRAFORM)
        executableVersion.set(TERRAFORM_DEFAULT)
    }
}