package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.ysb33r.gradle.terraform.tasks.base.AbstractTerraformTask

/**
 * @since 0.1
 */
@CompileStatic
class State {

    State(AbstractTerraformTask task) {

    }

    @Optional
    @InputFile
    File getPath() {
        null
    }
}
