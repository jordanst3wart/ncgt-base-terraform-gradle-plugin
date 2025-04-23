package org.ysb33r.gradle.terraform.config

interface ConfigExtension {
    val name: String
    fun getCommandLineArgs(): List<String>
}