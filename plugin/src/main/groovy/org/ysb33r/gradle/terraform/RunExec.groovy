package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.process.ExecSpec
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface RunExecParameters extends WorkParameters {
    Property<Project> getProject()
    Property<Action<ExecSpec>> getRunner()
    Property<File> getStdOut()
    Property<TerraformExecSpec> getExecSpec()
}

@CompileStatic
abstract class RunExec implements WorkAction<RunExecParameters> {
    @Override
    void execute() {
        // captures stdout out for show tasks, ie. terraform show
        if (parameters.stdOut.get()) {
            this.parameters.stdOut.get().withOutputStream { strm ->
                parameters.execSpec.get().standardOutput(strm)
                parameters.project.get().exec(parameters.runner.get()).assertNormalExitValue()
            }
        } else {
            parameters.project.get().exec(parameters.runner.get()).assertNormalExitValue()
        }
    }
}