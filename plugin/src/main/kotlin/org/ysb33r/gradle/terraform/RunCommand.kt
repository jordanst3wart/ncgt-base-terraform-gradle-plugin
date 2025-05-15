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
    fun getStdErrLog(): Property<File>
    fun getStdOutLog(): Property<File>
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
        logger.lifecycle("Running with TF_DATA_DIR: ${parameters.getEnvironment().get()["TF_DATA_DIR"]}, TF_CLI_CONFIG_FILE: ${parameters.getEnvironment().get()["TF_CLI_CONFIG_FILE"]}, TF_LOG_PATH: ${parameters.getEnvironment().get()["TF_LOG_PATH"]}, TF_LOG: ${parameters.getEnvironment().get()["TF_LOG"]}")
        processBuilder.redirectError(parameters.getStdErrLog().get())
        processBuilder.redirectOutput(parameters.getStdOutLog().get())
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        when (exitCode) {
            1 -> {
                // TODO readText reads everything into memory, which is not good for large files. It might need to be streamed in the future
                throw GradleException("Error (from ${
                    parameters.getStdErrLog().get().path}):\n${parameters.getStdErrLog().get().readText()}")
            }
            0 -> {
                logger.lifecycle("Successfully ran task")
            }
            2 -> {
                // TODO show stdout if drift...
                logger.warn("Changes detected (exitcode: $exitCode)")
                logger.lifecycle("output: (from ${parameters.getStdOutLog().get().path}):\n${
                    parameters.getStdOutLog().get().readText()}")
            }
            else -> {
                throw GradleException("Unknown exit code: $exitCode")
            }
        }
        if (parameters.getStdErrLog().get().isFile && parameters.getStdErrLog().get().length() == 0L) {
            parameters.getStdErrLog().get().delete()
        }
        if (parameters.getStdOutLog().get().isFile && parameters.getStdOutLog().get().length() == 0L) {
            parameters.getStdOutLog().get().delete()
        }
    }
}
