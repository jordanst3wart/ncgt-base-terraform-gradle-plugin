package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import java.io.File

/** Extension that describes a `terraformrc` file, cache directory, and downloads native binary.
 * find details about options here: https://developer.hashicorp.com/terraform/cli/config/config-file
 */
open class TerraformSetupExtension(project: Project) {
    companion object {
        const val TERRAFORM_SETUP_TASK = "setupTerraform"
    }

    var disableCheckPoint = true
    var disableCheckPointSignature = false
    var pluginCacheMayBreakDependencyLockFile = false

    val pluginCacheDir: DirectoryProperty = project.objects.directoryProperty()
    val terraformRC: RegularFile = project.layout.projectDirectory.file(".gradle/.terraformrc")

    init {
        pluginCacheDir.set(File(project.gradle.gradleUserHomeDir, "caches/terraform.d"))
    }
}