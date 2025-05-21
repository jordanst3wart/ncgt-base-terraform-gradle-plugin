package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.ExecSpec

import javax.inject.Inject

/** Equivalent of {@code terraform show /path/to/terraform.plan}.
 */
abstract class TerraformShow : TerraformTask {
    //@OutputFile
    //val statusReportOutputFile: Property<File> = project.objects.property(File::class.java)

    @Inject
    constructor() : super("show", emptyList()) {
        supportsColor(false)
        alwaysOutOfDate()
    }

    /** Whether output should be in JSON
     *
     * This option can be set from the command-line with {@code --json}.
     */
    @Option(option = "json", description = "Force output to be in JSON format")
    @Internal
    var json: Boolean = true

    override fun exec() {
        super.exec()
        //val fileLocation: URI = statusReportOutputFile.get().toURI()
        //logger.lifecycle(
        //    "The textual representation of the state file has been generated into $fileLocation"
        //)
    }

    override fun addCommandSpecificsToExecSpec(execSpec: ExecSpec): ExecSpec {
        super.addCommandSpecificsToExecSpec(execSpec)

        if (json) {
            execSpec.args.add(JSON_FORMAT)
        }
        execSpec.args.add(planFile.get().toPath().toAbsolutePath().toString())

        return execSpec
    }
}