package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.ysb33r.gradle.terraform.internal.Executable

/** Extension that describes a `terraformrc` file, cache directory, and downloads native binary.
 * find details about options here: https://developer.hashicorp.com/terraform/cli/config/config-file
 */
open class TerraformSetupExtension(project: Project) {
    companion object {
        const val TERRAFORM_SETUP_TASK = "setupTerraform"
    }

    val terraformRcMap = project.objects.mapProperty(String::class.java, Boolean::class.java)
    val executable = project.objects.property(Executable::class.java)

    init {
        terraformRcMap.set(
            mapOf(
                "plugin_cache_may_break_dependency_lock_file" to true,
                "disable_checkpoint" to true,
                "disable_checkpoint_signature" to true
            )
        )
        executable.set(Executable.TERRAFORM)
    }
}