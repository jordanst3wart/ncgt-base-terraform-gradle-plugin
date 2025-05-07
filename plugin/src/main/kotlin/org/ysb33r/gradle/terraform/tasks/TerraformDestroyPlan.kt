package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformExecSpec
import java.io.File

/**
 * Generates a destroy plan
 */
abstract class TerraformDestroyPlan : TerraformPlan() {

    override val planOutputFile: File
        get() = File(sourceSet.get().dataDir.get(), "${sourceSet.get().name}.tf.destroy.plan")

    override val variablesFile: Provider<File>
        get() = super.variablesFile.map {
            File(it.parentFile, "_d_.tfVars")
        }

    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs("-destroy")
        return execSpec
    }
}
