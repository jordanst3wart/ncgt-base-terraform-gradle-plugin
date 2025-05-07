package org.ysb33r.gradle.terraform.config

open class Parallel : ConfigExtension {
    override val name: String = "parallel"
    var maxParallel: Int = 10

    override fun getCommandLineArgs(): List<String> {
        return listOf("-parallelism=$maxParallel")
    }
}