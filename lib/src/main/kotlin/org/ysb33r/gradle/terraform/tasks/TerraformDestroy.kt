package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject
import java.io.File
import java.util.concurrent.Callable

/** Equivalent of {@code terraform destroy}.
 *  Note: DOES NOT USE A PLAN FILE
 */
abstract class TerraformDestroy : TerraformTask {
    @get:Internal
    val variablesFile: Provider<File> = project.provider(Callable {
        File(sourceSet.get().dataDir.get(), "__.tfvars")
    })

    @Inject
    constructor() : super("destroy", listOf(Lock::class.java, Refresh::class.java, Parallel::class.java, Json::class.java)) {
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        inputs.files(taskProvider("destroyPlan"))
        mustRunAfter(taskProvider("destroyPlan"))
        addCommandLineProvider(sourceSetVariables())
    }

    override fun exec() {
        createVarsFile()
        super.exec()
    }

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs("-var-file=${variablesFile.get().absolutePath}")
        return execSpec
    }

    private fun createVarsFile() {
        variablesFile.get().writer().use { writer ->
            tfVarProviders.map { it.get() }.flatten().forEach { writer.write(it) }
        }
    }
}