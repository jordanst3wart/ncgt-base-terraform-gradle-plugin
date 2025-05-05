package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.errors.MissingConfiguration
import org.ysb33r.gradle.terraform.internal.ConfigUtils.createPluginCacheDir
import java.io.File

/** Equivalent of [terraform init]. */
abstract class TerraformInit : TerraformTask {

    /**
     * Skip initialisation of child modules.
     */
    @get:Internal
    var skipChildModules: Boolean = false

    /**
     * The directory where terraform plugins data is written to.
     */
    @get:OutputDirectory
    val pluginDirectory: Provider<File>
        get() = sourceSet.map { source ->
            source.dataDir.map { File(it, "providers") }
        } as Provider<File>

    // TODO not having these assumes you are using a remote backend...
    /**
     * The location of [terraform.tfstate].
     */
    //@OutputFile
    //final Provider<File> terraformStateFile

    /**
     * The location of the file that provides details about the last run of this task.
     */
    //@OutputFile
    //final Provider<File> terraformInitStateFile

    constructor() : super("init", emptyList()) {
        supportsInputs()
        supportsColor()
        this.backendConfig = project.objects.property(File::class.java)
        this.useBackendConfig = project.objects.property(Boolean::class.java)
    }

    /** Configuration for Terraform backend.
     *
     * See [https://www.terraform.io/docs/backends/config.html#partial-configuration]
     *
     * @return Location of configuration file. Can be [null] if none is required.
     */
    @get:Optional
    @get:InputFile
    var backendConfigFile: Property<File> = project.objects.property(File::class.java)
        get() = this.backendConfig

    /** Set location of backend configuration file.
     *
     * @param backendFile Anything that can be converted using [project.file].
     */
    fun setBackendConfigFile(backendFile: File) {
        backendConfig.set(backendFile)
    }

    @get:Input
    var useBackendFile: Provider<Boolean> = project.objects.property(Boolean::class.java)
        get() = this.useBackendConfig

    override fun exec() {
        createPluginCacheDir(this.terraformrc)
        super.exec()
    }

    /** Add specific command-line options for the command.
     * If [--refresh-dependencies] was specified on the command-line the [-upgrade] will be passed
     * to [terraform init].
     *
     * @param execSpec
     * @return execSpec
     */
    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.cmdArgs("-get=${!skipChildModules}")

        if (!this.backendConfig.get().exists()) {
            throw MissingConfiguration("cannot location ${this.backendConfig.get().absolutePath}")
        }

        if (this.useBackendConfig.get()) {
            execSpec.cmdArgs("-backend-config=${this.backendConfig.get().absolutePath}")
        }

        return execSpec
    }

    private var useBackendConfig: Provider<Boolean>
    private val backendConfig: Property<File>
}