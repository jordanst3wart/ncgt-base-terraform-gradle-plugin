package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.core.ProjectOperations
import java.io.File
import java.util.concurrent.Callable

/**
 * Generates a remote state file containing partial configuration for backend.
 */
open class RemoteStateTask : DefaultTask() {

    private val projectOperations: ProjectOperations
    private val destinationDir: Property<File>
    private val outputFile: Property<File>
    private var backendText: Provider<String>

    init {
        description = "Generates configuration for backend state provider"
        destinationDir = project.objects.property(File::class.java)
        outputFile = project.objects.property(File::class.java)
        projectOperations = ProjectOperations.find(project)
        backendText = project.objects.property(String::class.java)
        outputFile.set(
            project.providers.provider(Callable<File> {
                File(destinationDir.get(), "backend-config.tf")
            })
        )
    }

    /**
     * Returns whether a backend partial configuration file should be generated.
     *
     * @return [true] is one is required.
     */
    @get:Internal
    val backendFileRequired: Provider<Boolean>
        get() {
            if (!backendText.isPresent) {
                return project.providers.provider { false }
            }
            return backendText.map {
                !it.isBlank()
            } as Provider<Boolean>
        }

    fun setBackendText(backendText: Provider<String>) {
        this.backendText = backendText
    }

    @get:Input
    val backendTextValue: Provider<String>
        get() = this.backendText

    /** Override the output directory.
     *
     * @param destDir Anything convertible to a file path.
     */
    fun setDestinationDir(destDir: File) {
        this.destinationDir.set(destDir)
    }

    @get:Internal
    val destinationDirValue: Property<File>
        get() = this.destinationDir

    /** The location of the backend configuration file.
     *
     * @return Configuration file.
     */
    @get:OutputFile
    val backendConfigFile: Property<File>
        get() = this.outputFile

    @TaskAction
    fun exec() {
        val backendFileContent = backendText.get()
        if (backendFileContent != null) {
            val outputFileActual = outputFile.get()
            outputFileActual.parentFile.mkdirs()
            outputFileActual.also {
                it.parentFile.mkdirs()
                it.writer().use { writer ->
                    writer.write(backendFileContent)
                }
            }
        } else {
            throw IllegalStateException("backend text is null")
        }
    }
}