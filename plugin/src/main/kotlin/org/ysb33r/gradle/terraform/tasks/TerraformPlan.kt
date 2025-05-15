package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject
import java.io.File
import java.util.concurrent.Callable

abstract class TerraformPlan : TerraformTask {

    @Inject
    constructor() : super(
        "plan",
        listOf(Lock::class.java, Refresh::class.java, Parallel::class.java, Json::class.java)
    ) {
        supportsInputs()
        supportsColor()
        alwaysOutOfDate() // plans are potentially always out of date if refresh=true
        addCommandLineProvider(sourceSetVariables())
    }

    /** Where the plan file will be written to.
     *
     * @return Location of plan file.
     */
    // TODO maybe should just be planFile
    @get:OutputFile
    open val planOutputFile: File
        get() = File(sourceSet.get().dataDir.get().asFile, "${sourceSet.get().name}.tf.plan")

    /** This is the location of an variables file used to keep anything provided via the build script.
     * @return Location of variables file.
     */
    @get:Internal
    open val variablesFile: Provider<File> = project.provider(Callable {
        File(sourceSet.get().dataDir.get().asFile, "__.tfvars")
    })

    override fun exec() {
        createVarsFile()
        super.exec()
        val planOut = planOutputFile
        logger.lifecycle(
            "generating plan file ${planOut.toURI()}"
        )
    }

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        if (project.hasProperty("tf.plan.refresh")) {
            logger.lifecycle("tf.plan.refresh property found setting refresh to false")
            extensions.getByType(Refresh::class.java).refresh = false
        }
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.apply {
            cmdArgs("-out=${planOutputFile}")
            cmdArgs("-var-file=${variablesFile.get().absolutePath}")
            cmdArgs("-detailed-exitcode")
        }
        return execSpec
    }

    private fun createVarsFile() {
        variablesFile.get().writer().use { writer ->
            tfVarProviders.map { it.get() }.flatten().forEach { writer.write(it) }
        }
    }
}