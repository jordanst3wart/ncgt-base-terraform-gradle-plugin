package org.ysb33r.gradle.terraform.internal

import org.ysb33r.gradle.terraform.internal.DownloaderOpenTofu.Companion.OS
import org.ysb33r.grashicorp.HashicorpUtils
import java.net.URI

data class DownloadBinaryData(val baseUrl: String, val toolExec: String, val version: String, val urlTemplate: (String, String, String) -> String) {

    fun uri(): URI? {
        val osArch = HashicorpUtils.osArch(OS)
        return URI(urlTemplate(baseUrl, version, osArch))
        //ie. return URI("${BASEURI}/download/v${version}/tofu_${version}_${osArch}.zip")
    }

    val exeName: String
        get() = if (OS.isWindows) "$toolExec.exe" else toolExec
}