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

/**
 * Generates a remote state file containing partial configuration for backend.
 */
open class RemoteStateTask : DefaultTask() {

    @get:OutputFile
    val backendConfig: Property<File> = project.objects.property(File::class.java)

    @get:Input
    val backendText: Property<String> = project.objects.property(String::class.java)

    init {
        description = "Generates configuration for backend state provider"
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

    @TaskAction
    fun exec() {
        val backendFileContent = backendText.get()
        if (backendFileContent != null) {
            val outputFileActual = backendConfig.get()
            outputFileActual.parentFile.mkdirs()
            outputFileActual.also {
                it.parentFile.mkdirs() // not needed
                it.writer().use { writer ->
                    writer.write(backendFileContent)
                }
            }
        } else {
            throw IllegalStateException("backend text is null")
        }
    }
}