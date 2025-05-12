package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath
import java.io.File
import java.io.Writer
import java.util.concurrent.Callable

/** Extension that describes a `terraformrc` file.
 * find details about options here: https://developer.hashicorp.com/terraform/cli/config/config-file
 */
open class TerraformRCExtension(project: Project) {
    companion object {
        const val TERRAFORM_RC_TASK = "generateTerraformConfig"
    }

    var disableCheckPoint = true
    var disableCheckPointSignature = false
    var useGlobalConfig = false
    var pluginCacheMayBreakDependencyLockFile = false

    private val pluginCacheDir: Property<File>
    private val terraformRC: Provider<File>
    private val projectOperations: ProjectOperations = ProjectOperations.maybeCreateExtension(project)

    init {
        this.terraformRC = project.providers.provider(
            Callable<File> {
                File(projectOperations.projectCacheDir, ".terraformrc")
            }
        )

        this.pluginCacheDir = project.objects.property(File::class.java)
        projectOperations.fsOperations.updateFileProperty(
            this.pluginCacheDir,
            projectOperations.gradleUserHomeDir.map { File(it, "caches/terraform.d") }
        )
    }

    /** Sets the location of the Terraform plugin cache directory
     *
     * @param dir Anything that is convertible using `project.file`.
     */
    fun setPluginCacheDir(dir: Any) {
        projectOperations.fsOperations.updateFileProperty(
            this.pluginCacheDir,
            projectOperations.provider(
                Callable<File> {
                    projectOperations.fsOperations.file(dir)
                }
            )
        )
    }

    /** Location of Terraform plugin cache directory
     *
     * @return Location of cache directory as a file provider.
     */
    fun getPluginCacheDir(): Provider<File> {
        return this.pluginCacheDir
    }

    /** Location of the `terraformrc` file if it should be written by the project.
     *
     * @return Location of `terraformrc` file. Never `null`.
     */
    fun getTerraformRC(): Provider<File> {
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
        writer.write("plugin_cache_dir = \"${escapedFilePath(OperatingSystem.current(), pluginCacheDir.get())}\"\n")
        writer.write("plugin_cache_may_break_dependency_lock_file = ${this.pluginCacheMayBreakDependencyLockFile}\n")
        return writer
    }
}