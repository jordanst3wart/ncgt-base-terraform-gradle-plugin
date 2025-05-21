package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.ysb33r.gradle.terraform.ExecSpec
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.config.Refresh

import javax.inject.Inject
import java.io.File

abstract class TerraformApply : TerraformTask {

    @InputFile
    val planFiles: Provider<File>

    @Inject
    constructor() : super("apply", listOf(Lock::class.java, Refresh::class.java, Parallel::class.java, Json::class.java)) {
        supportsAutoApprove()
        supportsInputs()
        supportsColor()
        planFiles = this.planFile
    }

    override fun addCommandSpecificsToExecSpec(execSpec: ExecSpec): ExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.args.add(planFiles.get().absolutePath)
        return execSpec
    }
}