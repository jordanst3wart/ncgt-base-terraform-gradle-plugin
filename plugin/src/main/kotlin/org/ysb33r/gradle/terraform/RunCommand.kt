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
    fun getCommands(): ListProperty<String>
    fun getWorkingDir(): Property<File>
    fun getErrorLog(): Property<File>
    // TODO add stdout, and rename stderr
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
        logger.lifecycle("Running with: TF_DATA_DIR: ${parameters.getEnvironment().get()["TF_DATA_DIR"]}, TF_CLI_CONFIG_FILE: ${parameters.getEnvironment().get()["TF_CLI_CONFIG_FILE"]}, TF_LOG_PATH: ${parameters.getEnvironment().get()["TF_LOG_PATH"]}, TF_LOG: ${parameters.getEnvironment().get()["TF_LOG"]}")
        processBuilder.redirectError(parameters.getErrorLog().get())
        processBuilder.redirectOutput(File("stdout.log"))
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
                // TODO show stdout if drift...
                logger.warn("Changes detected (exit code: $exitCode)")
            }
        }
        if (parameters.getErrorLog().get().isFile && parameters.getErrorLog().get().length() == 0L) {
            parameters.getErrorLog().get().delete()
            // TODO delete stdout.log if empty as well
        }
    }
}
