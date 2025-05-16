package org.ysb33r.gradle.terraform.internal

import org.gradle.api.file.DirectoryProperty
import org.gradle.internal.os.OperatingSystem
import java.io.File

object Utils {
    val OS: OperatingSystem = OperatingSystem.current()

    @JvmStatic
    fun terraformLogFile(name: String, logDir: DirectoryProperty): File {
        return File(logDir.get().asFile, "${name}.log").absoluteFile
    }

    @JvmStatic
    fun terraformStdErrLogFile(name: String, logDir: DirectoryProperty): File {
        return File(logDir.get().asFile, "${name}StdErr.log").absoluteFile
    }

    @JvmStatic
    fun terraformStdOutLogFile(name: String, logDir: DirectoryProperty): File {
        return File(logDir.get().asFile, "${name}StdOut.log").absoluteFile
    }

    @JvmStatic
    fun defaultEnvironment(): Map<String, String> {
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
