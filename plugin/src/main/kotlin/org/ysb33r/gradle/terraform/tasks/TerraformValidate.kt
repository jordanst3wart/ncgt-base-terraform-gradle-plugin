package org.ysb33r.gradle.terraform.tasks

import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.config.Json

import javax.inject.Inject

/** Equivalent of {@code terraform validate}.
 */
abstract class TerraformValidate : TerraformTask {

    @Inject
    constructor() : super("validate", listOf(Json::class.java)) {
        supportsColor()
    }
}