package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformExtension.Companion.TERRAFORM_DEFAULT
import org.ysb33r.gradle.terraform.TerraformSetupExtension
import org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath
import java.io.File
import java.io.Writer
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
import java.nio.file.attribute.PosixFilePermission.GROUP_READ
import java.nio.file.attribute.PosixFilePermission.GROUP_WRITE
import java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OTHERS_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.zip.ZipInputStream

open class TerraformSetup: DefaultTask() {

    @get:Input
    val terraformSetupExt: Property<TerraformSetupExtension> =
        project.objects.property(TerraformSetupExtension::class.java)

    @get:OutputFile
    val executable: Property<File> = project.objects.property(File::class.java)

    @get:OutputFile
    val terraformRC: RegularFile = terraformSetupExt.get().terraformRC

    @get:OutputDirectory
    val pluginCacheDir: DirectoryProperty = terraformSetupExt.get().pluginCacheDir

    init {
        // group = "Terraform"
        // description = "Setup Terraform environment"
        executable.set(terraformSetupExt.get().getExecutablePath())
    }

    @TaskAction
    fun exec() {
        createTerraformRcFile()
        createPluginCacheDir()
        downloadAndExtractZipFile()
        // TODO verify checksum, and verify signatures of downloaded file
    }

    private fun createTerraformRcFile(): RegularFile {
        terraformRC.asFile.bufferedWriter().use { writer ->
            this.toHCL(writer)
        }
        return terraformRC
    }

    private fun createPluginCacheDir(): DirectoryProperty {
        val rc = this.pluginCacheDir.get().asFile
        rc.mkdirs()
        if (OperatingSystem.current().isUnix) {
            Files.setPosixFilePermissions(
                rc.toPath(),
                setOf(
                    OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                    GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                    OTHERS_READ, OTHERS_EXECUTE
                )
            )
        }
        return this.pluginCacheDir
    }

    // TODO use HCL library to write tfvar file
    private fun toHCL(writer: Writer): Writer {
        writer.write("disable_checkpoint = ${terraformSetupExt.get().disableCheckPoint.get()}\n")
        writer.write("disable_checkpoint_signature = ${terraformSetupExt.get().disableCheckPointSignature.get()}\n")
        writer.write(
            "plugin_cache_dir = \"${
                escapedFilePath(
                    OperatingSystem.current(),
                    pluginCacheDir.get().asFile
                )
            }\"\n"
        )
        writer.write("plugin_cache_may_break_dependency_lock_file = ${terraformSetupExt.get().pluginCacheMayBreakDependencyLockFile.get()}\n")
        return writer
    }

    fun downloadAndExtractZipFile() {
        val executable = terraformSetupExt.get().executable.get()
        if (executable == null) {
            throw IllegalArgumentException("Executable not set, please set executable property")
        }
        val version = terraformSetupExt.get().executableVersion.get()
        if (version == TERRAFORM_DEFAULT) {
            logger.warn("using Terraform default version of $TERRAFORM_DEFAULT")
        }
        val url = executable.uriFromVersion(version)
        val outputFile = terraformSetupExt.get().getExecutablePath()
        outputFile.parentFile.mkdirs()
        val result = downloadUncompressAndCheck(url, outputFile)
        cleanupTempDirectory(result.second)
        logger.lifecycle("File from $url downloaded to: ${result.first.absolutePath}")
        if (OperatingSystem.current().isUnix) {
            Files.setPosixFilePermissions(
                outputFile.toPath(),
                setOf(
                    OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                    GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                    OTHERS_READ, OTHERS_EXECUTE
                )
            )
        }
        if (outputFile.absolutePath != result.first.absolutePath) {
            throw IllegalArgumentException("Output file does not match downloaded file: ${outputFile.absolutePath} != ${result.first.absolutePath}")
        }
        this.executable.set(result.first)
    }

    // zip extraction functions
    fun downloadUncompressAndCheck(url: URL, extractionPath: File): Pair<File,File> {
        val fileName = url.toString().substringAfterLast('/')
        if (fileName.isEmpty()) {
            throw IllegalArgumentException("Invalid URL: $url, filename is empty")
        }
        val tempDir = Files.createTempDirectory("download_").toFile()
        val tempFile = File(tempDir, fileName)

        val connection = url.openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.lifecycle("File from $url downloaded to tmp: ${tempFile.absolutePath}")

        if (isZipFile(tempFile)) {
            return Pair(uncompressZipFile(tempFile, extractionPath), tempDir)
        }
        throw IllegalArgumentException("File is not a zip file: $tempFile")
    }

    fun uncompressZipFile(zipFile: File, extractionPath: File): File {
        val extractedFiles = mutableListOf<File>()

        ZipInputStream(zipFile.inputStream()).use { zipInput ->
            val entry = zipInput.nextEntry
            if (entry != null) {
                throw IllegalArgumentException("Zip file is empty: $zipFile")
            }
            if (entry?.name == null) {
                throw IllegalArgumentException("Zip file entry name is null: $zipFile")
            }

            extractionPath.parentFile.mkdirs()
            val fileName = extractionPath.toString().substringAfterLast('/')
            if (fileName != zipFile.name) {
                throw IllegalArgumentException("Zip file name does not match: $fileName != ${zipFile.name}")
            }
            val entryFile = File(extractionPath.parentFile, entry.name)

            // Security check: prevent directory traversal attacks
            if (!entryFile.canonicalPath.startsWith(extractionPath.canonicalPath)) {
                throw SecurityException("Entry '${entry.name}' would extract outside of target directory")
            }

            if (entry.isDirectory) {
                throw IllegalArgumentException("Zip file contains a directory: ${entry.name}")
            } else {
                entryFile.parentFile?.mkdirs()
                entryFile.outputStream().use { output ->
                    zipInput.copyTo(output)
                }
                extractedFiles.add(entryFile)
                logger.lifecycle("Extracted file: ${entryFile.name} (${entryFile.length()} bytes)")
            }
        }

        return extractedFiles[0]
    }

    fun isZipFile(file: File): Boolean {
        return try {
            // Check file signature (magic bytes)
            val isZipByMagicBytes = checkZipMagicBytes(file)

            // Try to open as zip
            val isZipByOpening = tryOpenAsZip(file)

            isZipByMagicBytes && isZipByOpening
        } catch (_: Exception) {
            false
        }
    }

    fun checkZipMagicBytes(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val bytesRead = input.read(header)

                if (bytesRead >= 2) {
                    val byte1 = header[0].toInt() and 0xFF
                    val byte2 = header[1].toInt() and 0xFF

                    // Standard ZIP signature: 0x504B (PK)
                    byte1 == 0x50 && byte2 == 0x4B
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun tryOpenAsZip(file: File): Boolean {
        return try {
            ZipInputStream(file.inputStream()).use { zipInput ->
                val firstEntry = zipInput.nextEntry
                firstEntry != null
            }
        } catch (e: Exception) {
            false
        }
    }

    fun cleanupTempDirectory(directory: File) {
        try {
            directory.deleteRecursively()
            logger.lifecycle("Temporary directory cleaned up: ${directory.absolutePath}")
        } catch (e: Exception) {
            logger.error("Error cleaning up temporary directory: ${e.message}")
        }
    }
}