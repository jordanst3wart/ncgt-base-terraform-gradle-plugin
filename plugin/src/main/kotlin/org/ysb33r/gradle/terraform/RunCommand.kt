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

interface RunCommandParameters : WorkParameters {
    fun getEnvironment(): MapProperty<String, String>
    fun getCommands(): ListProperty<String> // TODO might change name
    fun getWorkingDir(): Property<File>
    fun getErrorLog(): Property<File>
}

abstract class RunCommand : WorkAction<RunCommandParameters> {
    val logger: Logger = Logging.getLogger(RunCommand::class.java)

    override fun execute() {
        val processBuilder = ProcessBuilder()
            .directory(parameters.getWorkingDir().get())
            .command(parameters.getCommands().get())
        processBuilder.environment().clear()
        processBuilder.environment().putAll(parameters.getEnvironment().get())
        logger.lifecycle("Running (environment redacted): ${processBuilder.command().joinToString(" ")}")
        // might be too much logging
        logger.lifecycle("Running with: TF_DATA_DIR: ${parameters.getEnvironment().get()["TF_DATA_DIR"]}, TF_CLI_CONFIG_FILE: ${parameters.getEnvironment().get()["TF_CLI_CONFIG_FILE"]}, TF_LOG_PATH: ${parameters.getEnvironment().get()["TF_LOG_PATH"]}, TF_LOG: ${parameters.getEnvironment().get()["TF_LOG"]}")
        processBuilder.redirectError(parameters.getErrorLog().get())
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        when (exitCode) {
            1 -> {
                logger.error("Failed (exit code: $exitCode)")
                throw GradleException("The process exited with exit code: $exitCode, error (from ${parameters.getErrorLog().get().path}):\n${parameters.getErrorLog().get().readText()}")
            }
            0 -> {
                logger.lifecycle("Succeeded (exit code: $exitCode)")
            }
            2 -> {
                // TODO process drift
                // log as both text, and json
                // print out text diffs to console
                // have json show file for processing
                // maybe log as a table or something, and ensure a json file is output
                logger.warn("Changes detected (exit code: $exitCode)")
            }
        }
    }
}
