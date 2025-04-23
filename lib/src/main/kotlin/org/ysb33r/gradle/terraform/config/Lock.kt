package org.ysb33r.gradle.terraform.config

class Lock() : ConfigExtension {
    override val name: String = "lock"

    var enabled: Boolean = true
    var timeout: Int = 30

    override fun getCommandLineArgs(): List<String> {
        return listOf("-lock=$enabled", "-lock-timeout=${timeout}s")
    }
}