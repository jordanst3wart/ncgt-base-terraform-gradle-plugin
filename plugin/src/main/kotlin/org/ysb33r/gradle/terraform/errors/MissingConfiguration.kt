package org.ysb33r.gradle.terraform.errors

import org.gradle.api.GradleException

class MissingConfiguration : GradleException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}