package org.ysb33r.gradle.terraform.config

open class Refresh : ConfigExtension {
    override val name = "refresh"
    var refresh = true

    override fun getCommandLineArgs(): List<String> {
        return listOf("-refresh=$refresh")
    }
}