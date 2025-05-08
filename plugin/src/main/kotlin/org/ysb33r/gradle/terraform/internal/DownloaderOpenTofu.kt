/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform.internal

import org.gradle.internal.os.OperatingSystem
import org.ysb33r.grashicorp.HashicorpUtils
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.errors.DistributionFailedException
import org.ysb33r.grolifant.api.v4.AbstractDistributionInstaller
import java.io.File
import java.net.URI

/** Downloads specific versions of `Terraform`.
 *
 * Currently limited to Windows (x86, x86_64), MacOS, Linux (x86, x86_64), Solaris (x86_64) and
 * FreeBSD (x86, x86_64).
 *
 * There are more binary packages are available from the Terraform site, but currently these are not being tested
 * not implemented. This includes:
 *
 * - linux_arm.zip
 * - freebsd_arm.zip
 * - openbsd_386, openbsd_amd64
 *
 * (Patches welcome!)
 */
class DownloaderOpenTofu(
    version: String,
    projectOperations: ProjectOperations
) : AbstractDistributionInstaller(TOOL_IDENTIFIER, version, "native-binaries/${TOOL_IDENTIFIER}", projectOperations),
    DownloaderBinary {

    companion object {
        val OS: OperatingSystem = OperatingSystem.current()
        const val TOOL_IDENTIFIER = "tofu"
        const val BASEURI = "https://github.com/opentofu/opentofu/releases"
    }

    /** Provides an appropriate URI to download a specific version of Terraform.
     *
     * @param ver Version of Terraform to download
     * @return URI for a supported platform; `null` otherwise.
     */
    override fun uriFromVersion(ver: String): URI? {
        val osArch = HashicorpUtils.osArch(OS)
        return if (osArch != null) {
            URI("${BASEURI}/download/v${ver}/tofu_${ver}_${osArch}.zip")
        } else {
            null
        }
    }

    /** Returns the path to the `terraform` executable.
     * Will force a download if not already downloaded.
     *
     * @return Location of `terraform` or null if not a supported operating system.
     */
    override fun terraformExecutablePath(): File {
        return distributionRoot?.let { File(it, exeName) } ?: throw NullPointerException("Distribution root is null")
    }

    /** Validates that the unpacked distribution is good.
     *
     * @param distDir Directory where distribution was unpacked to.
     * @param distributionDescription A descriptive name of the distribution
     * @return `distDir` as `Packer` distributions contains only a single executable.
     *
     * @throws DistributionFailedException if distribution failed to meet criteria.
     */
    override fun getAndVerifyDistributionRoot(distDir: File, distributionDescription: String): File {
        val checkFor = File(distDir, exeName)

        if (!checkFor.exists()) {
            throw DistributionFailedException(
                "${checkFor.name} not found in downloaded ${distributionDescription} distribution."
            )
        }

        return distDir
    }

    private val exeName: String
        get() = if (OS.isWindows) "tofu.exe" else "tofu"
}