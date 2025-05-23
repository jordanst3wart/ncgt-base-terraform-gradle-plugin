package org.ysb33r.gradle.terraform.internal

import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.internal.Convention.TERRAFORM_DEFAULT
import org.ysb33r.grashicorp.HashicorpUtils
import java.io.File
import java.net.URL

interface DownloadBinary {
    fun uriFromVersion(): URL
    fun executable(): String
    fun executablePath(): File
    // fun getAndVerifyDistributionRoot(distDir: File, distributionDescription: String): File
}

enum class Executable(val executable: String, val url: String, val version: String): DownloadBinary {
    TOFU("tofu", "https://github.com/opentofu/opentofu/releases", TERRAFORM_DEFAULT) {
        override fun uriFromVersion(): URL {
            val osArch = HashicorpUtils.osArch(OS)
            return URL("${url}/download/v${version}/tofu_${version}_${osArch}.zip")
        }
        override fun executable(): String {
            return if (OS.isWindows) "$executable.exe" else executable
        }
        override fun executablePath(): File {
            // TODO test for windows
            return File("${System.getProperty("user.home")}/.gradle/native/${executable}/$version/${executable()}")
        }
    },
    TERRAFORM("terraform", "https://releases.hashicorp.com/terraform", TERRAFORM_DEFAULT) {
        override fun uriFromVersion(): URL {;
            val osArch = HashicorpUtils.osArch(OS)
            return URL("${url}/${version}/terraform_${version}_${osArch}.zip")
        }
        override fun executable(): String {
            return if (OS.isWindows) "$executable.exe" else executable
        }
        override fun executablePath(): File {
            // TODO test for windows
            return File("${System.getProperty("user.home")}/.gradle/native/${executable}/$version/${executable()}")
        }
    };

    companion object {
        val OS: OperatingSystem = OperatingSystem.current()
    }
}
