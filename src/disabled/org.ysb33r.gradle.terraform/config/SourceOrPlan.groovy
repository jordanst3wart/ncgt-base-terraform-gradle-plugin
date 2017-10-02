package org.ysb33r.gradle.terraform.config

import groovy.transform.CompileStatic
import org.ysb33r.gradle.terraform.tasks.base.AbstractTerraformTask

/**
 * @since 0.1
 */
@CompileStatic
class SourceOrPlan {
    SourceOrPlan(AbstractTerraformTask task) {
        super(task)
    }

    void usePlan() {

    }

    void useSource() {

    }
}
