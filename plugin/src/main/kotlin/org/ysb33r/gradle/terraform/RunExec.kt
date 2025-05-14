package org.ysb33r.gradle.terraform

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File

interface RunExecParameters : WorkParameters {
    fun getEnvironment(): MapProperty<String, String>
    fun getCommands(): ListProperty<String> // TODO might change name
    fun getStdOut(): Property<File>
    fun getWorkingDir(): Property<File>
}

abstract class RunExec : WorkAction<RunExecParameters> {
    val logger: Logger = Logging.getLogger(RunExec::class.java)

    override fun execute() {
        // captures stdout out for show tasks, ie. terraform show
        /*if (parameters.getStdOut().get() != null) {
            parameters.getStdOut().get().outputStream().use { strm ->
                parameters.getExecSpec().get().standardOutput(strm)
                // parameters.getProject().get().exec(parameters.getRunner().get()).assertNormalExitValue()
            }
        } else {
            // parameters.getProject().get().exec(parameters.getRunner().get()).assertNormalExitValue()
        }*/
        val processBuilder = ProcessBuilder()
            .directory(parameters.getWorkingDir().get())
            .command(parameters.getCommands().get())
        processBuilder.environment().clear()
        processBuilder.environment().putAll(parameters.getEnvironment().get())
        logger.info("Running ${parameters.getEnvironment().get()}...")
        logger.info("running: ${processBuilder.command()} with environment: ${processBuilder.environment()}")
        logger.lifecycle("some lifecycle message")
        logger.error("Some error message")
        if (parameters.getStdOut().isPresent) {
            processBuilder.redirectOutput(parameters.getStdOut().get())
        } else {
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        }
        val process = processBuilder.start()
        // might not need a whenComplete
        process.onExit().whenComplete { result, error ->
            if (error != null) {
                throw GradleException("Error executing process", error)
            }
            assert(result.exitValue() == 0) { "Process failed with exit code ${result.exitValue()}" }
        }
    }
}
