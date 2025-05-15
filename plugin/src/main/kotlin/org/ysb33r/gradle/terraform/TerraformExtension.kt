package org.ysb33r.gradle.terraform

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.internal.DownloaderTerraform
import org.ysb33r.gradle.terraform.internal.DownloaderBinary
import org.ysb33r.gradle.terraform.internal.DownloaderOpenTofu
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.DownloadedExecutable
import org.ysb33r.grolifant.api.v4.exec.DownloaderFactory
import org.ysb33r.grolifant.api.v4.exec.ExternalExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolvableExecutable
import org.ysb33r.grolifant.api.v4.exec.ResolveExecutableByVersion
import org.ysb33r.grolifant.api.v4.exec.ResolverFactoryRegistry
import org.ysb33r.gradle.terraform.internal.Utils.awsEnvironment
import org.ysb33r.gradle.terraform.internal.Utils.googleEnvironment
import org.gradle.api.provider.Property

/** Configure project defaults or task specifics for `Terraform`.
 *
 * This also allows the `terraform` executable to be set
 *
 * It can be passed by a single map option.
 *
 * ```
 *   // By tag (Gradle will download and cache the correct distribution).
 *   executable(mapOf("tag" to "0.10.1"))
 *
 *   // By a physical path
 *   executable(mapOf("path" to "/path/to/terraform"))
 *
 *   // By using searchPath (will attempt to locate in search path).
 *   executable(mapOf("searchPath" to ""))
 * ```
 *
 * If the build runs on a platform that supports downloading of the `terraform` executable
 * the default will be to use the version as specified by [TerraformExtension.TERRAFORM_DEFAULT],
 * otherwise it will be in search mode.
 *
 * @author Schalk W. Cronjé
 *
 */
open class TerraformExtension(private val project: Project) {
    companion object {
        const val NAME = "terraform"
        const val TERRAFORM_DEFAULT = "1.8.0"
    }

    lateinit var resolvableExecutable: ResolvableExecutable

    val env = mutableMapOf<String, Any>()
    val registry: ResolverFactoryRegistry = ResolverFactoryRegistry(project)
    val projectOperations: ProjectOperations = ProjectOperations.maybeCreateExtension(project)
    val lock = Lock()
    val parallel = Parallel()
    val json = Json()
    val logLevel : Property<String> = project.objects.property(String::class.java)

    init {
        if (!DownloaderTerraform.isDownloadSupported()) {
            throw GradleException(
                "Terraform distribution not supported on ${OperatingSystem.current().name}"
            )
        }
        logLevel.set("INFO")
        addVersionResolver(projectOperations)
        executable(mapOf("version" to TERRAFORM_DEFAULT))
    }

    fun getResolver(): ExternalExecutable {
        return this.registry
    }

    fun executable(opts: Map<String, Any?>) {
        this.resolvableExecutable = this.registry.getResolvableExecutable(opts)
    }

    /** Replace current environment with new one.
     * If this is called on the task extension, no project extension environment will
     * be used.
     *
     * @param args New environment key-value map of properties.
     */
    fun setEnvironment(args: Map<String, Any>) {
        this.env.clear()
        this.env.putAll(args)
    }

    /** Environment for running the exe
     *
     * Calling this will resolve all lazy-values in the variable map.
     *
     * @return Map of environmental variables that will be passed.
     */
    fun getEnvironment(): Map<String, String> {
        return projectOperations.stringTools.stringizeValues(this.env)
    }

    /** Add environmental variables to be passed to the exe.
     *
     * @param args Environmental variable key-value map.
     */
    fun environment(args: Map<String, Any>) {
        this.env.putAll(args)
    }

    fun useAwsEnvironment() {
        environment(awsEnvironment())
    }

    fun useGoogleEnvironment() {
        environment(googleEnvironment())
    }

    fun setLockTimeout(timeout: Int) {
        this.lock.timeout = timeout
        this.lock.enabled = true
    }

    fun setParallel(parallel: Int) {
        this.parallel.maxParallel = parallel
    }

    fun setJson(enabled: Boolean) {
        this.json.enabled = enabled
    }

    @Suppress("UNCHECKED_CAST")
    private fun addVersionResolver(projectOperations: ProjectOperations) {
        val tofu = project.rootProject.properties.getOrDefault("opentofu", false)
        val resolver = DownloadedExecutable { installer: DownloaderBinary -> installer.terraformExecutablePath() }
        if (tofu == true) {
            val downloaderFactory = DownloaderFactory { options, version, p -> DownloaderOpenTofu(version, p) }
            this.registry.registerExecutableKeyActions(
                ResolveExecutableByVersion<DownloaderOpenTofu>(projectOperations, downloaderFactory, resolver)
            )
        } else {
            val downloaderFactory = DownloaderFactory { options, version, p -> DownloaderTerraform(version, p) }
            this.registry.registerExecutableKeyActions(
                ResolveExecutableByVersion<DownloaderTerraform>(projectOperations, downloaderFactory, resolver)
            )
        }
    }
}