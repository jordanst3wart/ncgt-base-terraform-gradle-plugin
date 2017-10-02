package org.ysb33r.gradle.terraform.config

import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.ysb33r.gradle.terraform.tasks.base.AbstractTerraformTask

/**
 * @since
 */
class StateOut extends State {
    StateOut(AbstractTerraformTask task) {
        super(task)
    }

    @Optional
    @OutputFile
    File getStateOut() {
        null
    }
}
