package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.ExecSpec

/** The [terraform fmt -check] command.
 */
abstract class TerraformFmtCheck : TerraformTask {

    constructor() : super("fmt", emptyList())

    @get:Input
    var recursive: Boolean = true

    override fun addCommandSpecificsToExecSpec(execSpec: ExecSpec): ExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.args.add("-check")

        if (logger.isInfoEnabled) {
            execSpec.args.add("-diff")
        }

        if (!logger.isQuietEnabled) {
            execSpec.args.add("-list=true")
        }

        if (recursive) {
            execSpec.args.add("-recursive")
        }

        return execSpec
    }
}