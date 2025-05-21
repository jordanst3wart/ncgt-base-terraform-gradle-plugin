package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.tasks.Input
import org.ysb33r.gradle.terraform.ExecSpec

/** The [terraform fmt -write=true] command. */
abstract class TerraformFmtApply : TerraformTask {

    constructor() : super("fmt", emptyList())

    @get:Input
    var recursive: Boolean = true

    override fun addCommandSpecificsToExecSpec(execSpec: ExecSpec): ExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        execSpec.args.add("-write=true")

        if (recursive) {
            execSpec.args.add("-recursive")
        }

        if (logger.isInfoEnabled) {
            execSpec.args.add("-list=true")
        } else {
            execSpec.args.add("-list=false")
        }

        return execSpec
    }
}