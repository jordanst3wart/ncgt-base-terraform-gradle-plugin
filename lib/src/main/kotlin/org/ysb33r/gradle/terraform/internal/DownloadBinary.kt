package org.ysb33r.gradle.terraform.internal

import java.io.File

interface DownloaderBinary {
    fun terraformExecutablePath(): File
}