package org.ysb33r.gradle.terraform.internal

import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformRCExtension
import java.io.File

object Utils {
    val OS: OperatingSystem = OperatingSystem.current()

    @JvmStatic
    fun awsEnvironment(): Map<String, String> {
        return System.getenv().filterKeys { it.startsWith("AWS_") }
    }

    @JvmStatic
    fun googleEnvironment(): Map<String, String> {
        return System.getenv().filterKeys { it.startsWith("GOOGLE_") }
    }

    @JvmStatic
    fun terraformEnvironment(
        terraformrc: TerraformRCExtension,
        name: String,
        dataDir: Provider<File>,
        logDir: Provider<File>,
        logLevel: String
    ): Map<String, String> {
        val environment = mutableMapOf(
            "TF_DATA_DIR" to dataDir.get().absolutePath,
            "TF_CLI_CONFIG_FILE" to ConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
            "TF_LOG_PATH" to terraformLogFile(name, logDir).absolutePath,
            "TF_LOG" to (logLevel ?: "")
        )
        environment.putAll(defaultEnvironment())
        return environment
    }

    @JvmStatic
    fun terraformLogFile(name: String, logDir: Provider<File>): File {
        return File(logDir.get(), "${name}.log").absoluteFile
    }

    private fun defaultEnvironment(): Map<String, String> {
        return if (OS.isWindows) {
            mapOf(
                "TEMP" to (System.getenv("TEMP") ?: ""),
                "TMP" to (System.getenv("TMP") ?: ""),
                "HOMEDRIVE" to (System.getenv("HOMEDRIVE") ?: ""),
                "HOMEPATH" to (System.getenv("HOMEPATH") ?: ""),
                "USERPROFILE" to (System.getenv("USERPROFILE") ?: ""),
                OS.pathVar to (System.getenv(OS.pathVar) ?: "")
            )
        } else {
            mapOf(
                "HOME" to System.getProperty("user.home"),
                OS.pathVar to (System.getenv(OS.pathVar) ?: "")
            )
        }
    }
}
