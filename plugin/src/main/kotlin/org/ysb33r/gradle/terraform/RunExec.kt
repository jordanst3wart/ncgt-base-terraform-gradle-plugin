package org.ysb33r.gradle.terraform

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.process.ExecSpec
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

interface RunExecParameters : WorkParameters {
    fun getProject(): Property<Project>
    fun getRunner(): Property<Action<ExecSpec>>
    fun getStdOut(): Property<File>
    fun getExecSpec(): Property<TerraformExecSpec>
}

abstract class RunExec : WorkAction<RunExecParameters> {
    override fun execute() {
        // captures stdout out for show tasks, ie. terraform show
        if (parameters.getStdOut().get() != null) {
            parameters.getStdOut().get().outputStream().use { strm ->
                parameters.getExecSpec().get().standardOutput(strm)
                parameters.getProject().get().exec(parameters.getRunner().get()).assertNormalExitValue()
            }
        } else {
            parameters.getProject().get().exec(parameters.getRunner().get()).assertNormalExitValue()
        }
    }
}