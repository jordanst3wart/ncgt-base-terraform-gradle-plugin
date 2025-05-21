package org.ysb33r.grashicorp

import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.util.regex.Pattern

object HashicorpUtils {
    private val BACKSLASH: Pattern = Pattern.compile("\\x5C")
    private const val DOUBLE_BACKSLASH = "\\\\\\\\"

    @JvmStatic
    fun osArch(os: OperatingSystem): String {
        val osName = when {
            os.isWindows -> "windows"
            os.isLinux -> "linux"
            os.isMacOsX -> "darwin"
            else -> throw IllegalArgumentException("OS is not supported: $os")
        }
        val arch = getArch()
        return "${osName}_${arch}"
    }

    @JvmStatic
    fun getArch(): String {
        val arch = System.getProperty("os.arch").lowercase()

        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "arm64"
            arch.contains("x86_64") || arch.contains("amd64") -> "amd64"
            arch.contains("x86") || arch.contains("i386") || arch.contains("i686") -> "386"
            else -> throw IllegalStateException("Unsupported architecture: $arch")
        }
    }

    @JvmStatic
    fun escapedFilePath(os: OperatingSystem, path: File): String {
        return if (os.isWindows) {
            path.absolutePath.replace(BACKSLASH.toRegex(), DOUBLE_BACKSLASH)
        } else {
            path.absolutePath
        }
    }
}