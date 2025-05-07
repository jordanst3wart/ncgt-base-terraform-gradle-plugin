package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.TerraformExecSpec

/** The [terraform fmt -write=true] command. */
abstract class TerraformFmtApply : TerraformTask {

    constructor() : super("fmt", emptyList())

    @get:Input
    var recursive: Boolean = true

    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.cmdArgs("-write=true")

        if (recursive) {
            execSpec.cmdArgs("-recursive")
        }

        if (logger.isInfoEnabled) {
            execSpec.cmdArgs("-list=true")
        } else {
            execSpec.cmdArgs("-list=false")
        }

        return execSpec
    }
}