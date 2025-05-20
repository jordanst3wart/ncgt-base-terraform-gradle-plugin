package org.ysb33r.gradle.terraform

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface RunCommandParameters : WorkParameters {
    fun getEnvironment(): MapProperty<String, String>
    fun getCommands(): ListProperty<String>
    fun getWorkingDir(): DirectoryProperty
    fun getStdErrLog(): RegularFileProperty
    fun getStdOutLog(): RegularFileProperty
}

abstract class RunCommand : WorkAction<RunCommandParameters> {
    val logger: Logger = Logging.getLogger(RunCommand::class.java)

    override fun execute() {
        val processBuilder = ProcessBuilder()
            .directory(parameters.getWorkingDir().get().asFile)
            .command(parameters.getCommands().get())
        processBuilder.environment().clear()
        processBuilder.environment().putAll(parameters.getEnvironment().get())
        logger.lifecycle("Running (environment redacted): ${processBuilder.command().joinToString(" ")}")
        logger.lifecycle("Running with TF_DATA_DIR: ${parameters.getEnvironment().get()["TF_DATA_DIR"]}, TF_CLI_CONFIG_FILE: ${parameters.getEnvironment().get()["TF_CLI_CONFIG_FILE"]}, TF_LOG_PATH: ${parameters.getEnvironment().get()["TF_LOG_PATH"]}, TF_LOG: ${parameters.getEnvironment().get()["TF_LOG"]}")
        processBuilder.redirectError(parameters.getStdErrLog().get().asFile)
        processBuilder.redirectOutput(parameters.getStdOutLog().get().asFile)
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        when (exitCode) {
            1 -> {
                // TODO readText reads everything into memory, which is not good for large files. It might need to be streamed in the future
                throw GradleException("Error (from ${
                    parameters.getStdErrLog().get().asFile.path}):\n${parameters.getStdErrLog().get().asFile.readText()}")
            }
            0 -> {
                // TODO could stream stdout to the console, or just read it, show warnings
                // trying just warnings
                logger.lifecycle("Successfully ran task")
                if (parameters.getStdErrLog().get().asFile.isFile && parameters.getStdErrLog().get().asFile.length() > 0L) {
                    logger.lifecycle("output: (from ${parameters.getStdErrLog().get().asFile.path}):\n${
                        parameters.getStdErrLog().get().asFile.readText()}")
                }
            }
            2 -> {
                // TODO show stdout if drift...
                logger.warn("Changes detected (exitcode: $exitCode)")
                logger.lifecycle("output: (from ${parameters.getStdOutLog().get().asFile.path}):\n${
                    parameters.getStdOutLog().get().asFile.readText()}")
            }
            else -> {
                throw GradleException("Unknown exit code: $exitCode")
            }
        }
        if (parameters.getStdErrLog().get().asFile.isFile && parameters.getStdErrLog().get().asFile.length() == 0L) {
            parameters.getStdErrLog().get().asFile.delete()
        }
        if (parameters.getStdOutLog().get().asFile.isFile && parameters.getStdOutLog().get().asFile.length() == 0L) {
            parameters.getStdOutLog().get().asFile.delete()
        }
    }
}
