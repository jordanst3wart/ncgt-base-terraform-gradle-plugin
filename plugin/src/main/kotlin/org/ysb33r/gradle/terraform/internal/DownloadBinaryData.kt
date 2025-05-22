package org.ysb33r.gradle.terraform.internal

import org.gradle.internal.os.OperatingSystem
import org.ysb33r.grashicorp.HashicorpUtils
import java.io.File
import java.net.URL

interface DownloadBinary {
    fun uriFromVersion(ver: String): URL
    fun executable(): String
    fun executablePath(ver: String): File
    // fun getAndVerifyDistributionRoot(distDir: File, distributionDescription: String): File
}

enum class Executable(val executable: String, val url: String): DownloadBinary {
    TOFU("tofu", "https://github.com/opentofu/opentofu/releases") {
        override fun uriFromVersion(ver: String): URL {
            val osArch = HashicorpUtils.osArch(OS)
            return URL("${url}/download/v${ver}/tofu_${ver}_${osArch}.zip")
        }
        override fun executable(): String {
            return if (OS.isWindows) "$executable.exe" else executable
        }
        override fun executablePath(ver: String): File {
            // TODO test for windows
            return File("${System.getProperty("user.home")}/.gradle/native/${executable}/$ver/${executable()}")
        }
    },
    TERRAFORM("terraform", "https://releases.hashicorp.com/terraform") {
        override fun uriFromVersion(ver: String): URL {;
            val osArch = HashicorpUtils.osArch(OS)
            return URL("${url}/${ver}/terraform_${ver}_${osArch}.zip")
        }
        override fun executable(): String {
            return if (OS.isWindows) "$executable.exe" else executable
        }
        override fun executablePath(ver: String): File {
            // TODO test for windows
            return File("${System.getProperty("user.home")}/.gradle/native/${executable}/$ver/${executable()}")
        }
    };

    companion object {
        val OS: OperatingSystem = OperatingSystem.current()
    }
}
