package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.ExecSpec

import javax.inject.Inject
import java.io.File
import java.net.URI

/** Equivalent of {@code terraform show}.
 * shows the state of the terraform plan
 */
abstract class TerraformShowState : TerraformTask {
    @OutputFile
    val statusReportOutputFile: Property<File> = project.objects.property(File::class.java)

    @Inject
    constructor() : super("show", emptyList()) {
        statusReportOutputFile.set(
            project.provider {
                File(sourceSet.get().reportsDir.get().asFile,
                    "${sourceSet.get().name}.status.${if (json) "tf.json" else "tf"}")
            })

        supportsColor(false)
        alwaysOutOfDate()
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    @Option(option = "json", description = "Force output to be in JSON format")
    @Internal
    var json: Boolean = false

    override fun exec() {
        super.exec()
        val fileLocation: URI = statusReportOutputFile.get().toURI()
        logger.lifecycle(
            "The textual representation of the state file has been generated into $fileLocation"
        )
    }

    override fun addCommandSpecificsToExecSpec(execSpec: ExecSpec): ExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.args.add(JSON_FORMAT)
        }

        return execSpec
    }
}