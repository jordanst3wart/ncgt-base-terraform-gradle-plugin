package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.TerraformExecSpec

/** The [terraform fmt -check] command.
 */
abstract class TerraformFmtCheck : TerraformTask {

    constructor() : super("fmt", emptyList())

    @get:Input
    var recursive: Boolean = true

    override fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.cmdArgs("-check")

        if (logger.isInfoEnabled) {
            execSpec.cmdArgs("-diff")
        }

        if (!logger.isQuietEnabled) {
            execSpec.cmdArgs("-list=true")
        }

        if (recursive) {
            execSpec.cmdArgs("-recursive")
        }

        return execSpec
    }
}