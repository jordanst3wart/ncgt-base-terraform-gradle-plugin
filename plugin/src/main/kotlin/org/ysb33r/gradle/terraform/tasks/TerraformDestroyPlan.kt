package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.io.writer

/**
 * Generates a destroy plan
 */
abstract class TerraformDestroyPlan : TerraformTask {

    // TODO is @inject needed here?
    @Inject
    constructor() : super(
        "destroyPlan",
        listOf(Lock::class.java, Refresh::class.java, Parallel::class.java, Json::class.java)
    ) {
        supportsInputs()
        supportsColor()
        alwaysOutOfDate() // plans are potentially always out of date if refresh=true
        addCommandLineProvider(sourceSetVariables())
    }

    val planOutputFile: File
        get() = File(sourceSet.get().dataDir.get().asFile, "${sourceSet.get().name}.tf.destroy.plan")

    @get:Internal
    open val variablesFile: Provider<File> = project.provider(Callable {
        File(sourceSet.get().dataDir.get().asFile, "_d_.tfvars")
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
            cmdArgs("-destroy")
        }
        return execSpec
    }

    private fun createVarsFile() {
        variablesFile.get().writer().use { writer ->
            tfVarProviders.map { it.get() }.flatten().forEach { writer.write(it) }
        }
    }
}
