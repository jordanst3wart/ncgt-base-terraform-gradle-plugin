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
import java.io.File

abstract class TerraformInit : TerraformTask {

    @get:Internal
    var skipChildModules: Boolean = false


    @Input
    val useBackendConfig: Property<Boolean>

    @get:Optional
    @get:InputFile
    val backendConfig: Property<File>

    /**
     * The directory where terraform plugins data is written to.
     */
    @get:OutputDirectory
    val pluginDirectory: Provider<File>
        get() = sourceSet.map { source ->
            source.dataDir.map { File(it.asFile, "providers") }
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
        alwaysOutOfDate()
        backendConfig = project.objects.property(File::class.java)
        useBackendConfig = project.objects.property(Boolean::class.java)
    }

    override fun exec() {
        this.terraformRc.createPluginCacheDir()
        super.exec()
    }

    /** Add specific command-line options for the command.
     * If [--refresh-dependencies] was specified on the command-line the [-upgrade] will be passed
     * to [terraform init].
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
}