package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.problems.PropertyTrace
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformExtension.Companion.TERRAFORM_DEFAULT
import org.ysb33r.gradle.terraform.TerraformSetupExtension
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

open class TerraformSetup: DefaultTask() {

    @get:Input
    val terraformSetupExt: Property<TerraformSetupExtension> = project.objects.property(TerraformSetupExtension::class.java)

    @get:OutputFile
    val executable: Property<File> = project.objects.property(File::class.java)

    @get:OutputFile
    val terraformRC: RegularFile = terraformSetupExt.get().terraformRC

    @get:OutputDirectory
    val pluginCacheDir: DirectoryProperty = terraformSetupExt.get().pluginCacheDir

    @TaskAction
    fun exec() {
        createTerraformRcFile()
        createPluginCacheDir()
        downloadAndExtractZipFile()
        // TODO download binary
        // TODO verify checksum, and verify signatures, and things
    }

    private fun createTerraformRcFile(): RegularFile {
        terraformRC.asFile.bufferedWriter().use { writer ->
            this.toHCL(writer)
        }
        return terraformRC
    }

    // should move to a task
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

    private fun toHCL(writer: Writer): Writer {
        writer.write("disable_checkpoint = ${terraformSetupExt.get().disableCheckPoint.get()}\n")
        writer.write("disable_checkpoint_signature = ${terraformSetupExt.get().disableCheckPointSignature.get()}\n")
        writer.write("plugin_cache_dir = \"${escapedFilePath(OperatingSystem.current(), pluginCacheDir.get().asFile)}\"\n")
        writer.write("plugin_cache_may_break_dependency_lock_file = ${terraformSetupExt.get().pluginCacheMayBreakDependencyLockFile.get()}\n")
        return writer
    }

    // TODO error handling
    // TODO unzip if .zip file
    fun downloadAndExtractZipFile() {
        val executable = terraformSetupExt.get().executable.get()
        if (executable == null){
            throw IllegalArgumentException("Executable not set, please set executable property")
        }
        val version = terraformSetupExt.get().executableVersion.get()
        if (version == TERRAFORM_DEFAULT) {
            logger.warn("using Terraform default version of $TERRAFORM_DEFAULT")
        }
        val url = executable.uriFromVersion(version)
        val destinationPath = executable.executablePath(version)

        val connection = url.openConnection()
        connection.connect()

        val inputStream = connection.getInputStream()
        val outputFile = destinationPath.asFile

        inputStream.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        println("File downloaded successfully to: $destinationPath")
    }
}