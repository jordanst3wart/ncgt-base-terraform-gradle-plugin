package org.ysb33r.gradle.terraform.internal

import org.gradle.internal.os.OperatingSystem
import org.ysb33r.grashicorp.HashicorpUtils
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.errors.DistributionFailedException
import org.ysb33r.grolifant.api.v4.AbstractDistributionInstaller
import java.io.File
import java.net.URI

class DownloaderTerraform(
    version: String,
    projectOperations: ProjectOperations
) : AbstractDistributionInstaller(
    TOOL_IDENTIFIER,
    version,
    "native-binaries/${TOOL_IDENTIFIER}",
    projectOperations
), DownloaderBinary {

    companion object {
        val OS: OperatingSystem = OperatingSystem.current()
        const val TOOL_IDENTIFIER = "terraform"
        val BASEURI: String = HashicorpUtils.getDownloadBaseUri(TOOL_IDENTIFIER)

        @JvmStatic
        fun isDownloadSupported(): Boolean {
            return (OS.isWindows || OS.isLinux || OS.isMacOsX)
        }
    }

    override fun uriFromVersion(ver: String): URI {
        val osArch = HashicorpUtils.osArch(OS)
        return URI("${BASEURI}/${ver}/terraform_${ver}_${osArch}.zip")
    }

    override fun terraformExecutablePath(): File {
        return distributionRoot?.let { File(it, exeName) } ?: throw NullPointerException("Distribution root is null")
    }

    override fun getAndVerifyDistributionRoot(distDir: File, distributionDescription: String): File {
        val checkFor = File(distDir, exeName)

        if (!checkFor.exists()) {
            throw DistributionFailedException(
                "${checkFor.name} not found in downloaded $distributionDescription distribution."
            )
        }

        return distDir
    }

    private val exeName: String
        get() = if (OS.isWindows) "terraform.exe" else "terraform"
}