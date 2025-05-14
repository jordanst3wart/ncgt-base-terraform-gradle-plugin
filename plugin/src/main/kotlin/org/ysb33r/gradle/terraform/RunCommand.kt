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
    fun getStdOut(): Property<File>
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
        // logger.info("Running ${parameters.getEnvironment().get()}...")
        logger.lifecycle("Running (environment redacted): ${processBuilder.command().joinToString(" ")}")

        // TODO potentially log:
        //             "TF_DATA_DIR" to dataDir.get().absolutePath,
        //            "TF_CLI_CONFIG_FILE" to ConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
        //            "TF_LOG_PATH" to terraformLogFile(name, logDir).absolutePath,
        //            "TF_LOG" to logLevel

        processBuilder.redirectError(parameters.getErrorLog().get())
        val process = processBuilder.start()
        val exitCode = process.waitFor()
        when (exitCode) {
            1 -> {
                logger.error("Failed (exit code: $exitCode)")
                throw GradleException("The process exited with exit code: $exitCode, error (from ${parameters.getErrorLog().get().path}):\n ${parameters.getErrorLog().get().readText()}")
            }
            0 -> {
                logger.lifecycle("Succeeded (exit code: $exitCode)")
            }
            2 -> {
                // TODO process drift
                logger.warn("The process exited with exit code: $exitCode, potential drift detected")
            }
        }
    }
}
