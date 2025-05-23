package org.ysb33r.gradle.terraform

import org.gradle.api.Project
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
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
    }

    val env = mutableMapOf<String, String>()
    val lock = Lock()
    val parallel = Parallel()
    val json = Json()
    val logLevel : Property<String> = project.objects.property(String::class.java)

    init {
        logLevel.set("WARN")
    }

    /** Replace current environment with new one.
     * If this is called on the task extension, no project extension environment will
     * be used.
     *
     * @param args New environment key-value map of properties.
     */
    fun setEnvironment(args: Map<String, String>) {
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
        return this.env
    }

    /** Add environmental variables to be passed to the exe.
     *
     * @param args Environmental variable key-value map.
     */
    fun environment(args: Map<String, String>) {
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

    private fun awsEnvironment(): Map<String, String> {
        return System.getenv().filterKeys { it.startsWith("AWS_") }
    }

    private fun googleEnvironment(): Map<String, String> {
        return System.getenv().filterKeys { it.startsWith("GOOGLE_") }
    }
}