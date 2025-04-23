package org.ysb33r.gradle.terraform.config

interface ConfigExtension {
    val name: String
    val commandLineArgs: List<String>
}