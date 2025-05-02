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
 *
 * @author Schalk W. Cronjé
 */
class TerraformRCExtension(project: Project) {
    companion object {
        const val TERRAFORM_RC_TASK = "generateTerraformConfig"
    }

    /** Disable checkpoint.
     *
     * When set to true, disables upgrade and security bulletin checks that require reaching out to
     * HashiCorp-provided network services.
     *
     * Default is `true`.
     */
    var disableCheckPoint = true

    /** Disable checkpoint signature.
     *
     * When set to true, allows the upgrade and security bulletin checks described above but disables the use of an
     * anonymous id used to de-duplicate warning messages.
     *
     * Default is `false`.
     */
    var disableCheckPointSignature = false

    /** Select source of Terraform configuration.
     *
     * When set to `true` use global Terraform configuration rather than a project configuration.
     *
     * Default is `true`.
     */
    var useGlobalConfig = false

    /** Plugin cache may break dependency lock file.
     *
     * Setting this option gives Terraform CLI permission to create an incomplete dependency
     * lock file entry for a provider if that would allow Terraform to use the cache to install that provider.
     *
     * In that situation the dependency lock file will be valid for use on the current system but may not be
     * valid for use on another computer with a different operating system or CPU architecture, because it
     * will include only a checksum of the package in the global cache.
     *
     * Default is `false`.
     */
    var pluginCacheMayBreakDependencyLockFile = false

    private val pluginCacheDir: Property<File>
    private val terraformRC: Provider<File>
    private val credentials = mutableMapOf<String, String>()
    private val projectOperations: ProjectOperations

    init {
        this.projectOperations = ProjectOperations.find(project)

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

    /** Adds a credential set to the configuration file.
     *
     * @param key Remote Terraform system name
     * @param token Token for remote Terraform system.
     */
    fun credentials(key: String, token: String) {
        credentials[key] = token
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
        this.credentials.forEach { (key, token) ->
            writer.write("credentials \"${key}\" {\n")
            writer.write("  token = \"${token}\"\n")
            writer.write("}\n")
        }
        return writer
    }
}