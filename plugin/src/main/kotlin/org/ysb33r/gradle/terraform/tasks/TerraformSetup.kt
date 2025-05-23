package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.internal.Convention
import org.ysb33r.gradle.terraform.internal.Executable
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
    val terraformRcMap = project.objects.mapProperty(String::class.java, Boolean::class.java)

    @get:Input
    val executable: Property<Executable> = project.objects.property(Executable::class.java)

    @get:OutputFile
    @Optional
    val executableFile: RegularFileProperty = project.objects.fileProperty()

    @get:OutputFile
    val terraformRC: RegularFileProperty = project.objects.fileProperty()

    @get:OutputDirectory
    val pluginCacheDir: DirectoryProperty = project.objects.directoryProperty()

    init {
        group = Convention.TERRAFORM_TASK_GROUP
        description = "Generates Terraform rc file, creates plugin cache directory, and downloads binary"
        pluginCacheDir.set(Convention.pluginCacheDir(project))
        terraformRC.set(Convention.terraformRC(project))
        // executableFile.set(Executable.TERRAFORM.executablePath())
    }

    @TaskAction
    fun exec() {
        createTerraformRcFile()
        createPluginCacheDir()
        downloadAndExtractZipFile()
        // TODO verify checksum, and verify signatures of downloaded file
    }

    private fun createTerraformRcFile() {
        terraformRC.get().asFile.bufferedWriter().use { writer ->
            this.toHCL(writer)
        }
    }

    private fun createPluginCacheDir(): DirectoryProperty {
        val rc = pluginCacheDir.get().asFile
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
        terraformRcMap.get().forEach {
            // TODO should throw if value is a Map, or list. I don't know if that is parsed correctly
            writer.write("${it.key} = ${it.value}\n")
        }
        writer.write(
            "plugin_cache_dir = \"${
                escapedFilePath(
                    OperatingSystem.current(),
                    pluginCacheDir.get().asFile
                )
            }\"\n"
        )
        return writer
    }

    private fun downloadAndExtractZipFile() {
        val executableObj = executable.get()
        executableFile.set(executableObj.executablePath())
        executableObj.executablePath().parentFile.mkdirs()
        if (executableObj.version == Convention.TERRAFORM_DEFAULT) {
            logger.warn("Using Terraform default version of ${Convention.TERRAFORM_DEFAULT}, you should set a version")
        }
        val url = executableObj.uriFromVersion()
        val result = downloadUncompressAndCheck(url, executableFile.get().asFile)
        cleanupTempDirectory(result.second)
        logger.lifecycle("File from $url downloaded to: ${result.first.absolutePath}")
        if (OperatingSystem.current().isUnix) {
            Files.setPosixFilePermissions(
                executableFile.get().asFile.toPath(),
                setOf(
                    OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                    GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                    OTHERS_READ, OTHERS_EXECUTE
                )
            )
        }
        if (executableFile.get().asFile.absolutePath != result.first.absolutePath) {
            throw IllegalArgumentException("Output file does not match downloaded file: ${executableFile.get().asFile.absolutePath} != ${result.first.absolutePath}")
        }
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
        } catch (_: Exception) {
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