package org.ysb33r.gradle.terraform

// working directory is included in the sourceSet
data class ExecSpec(val executable: String, val command: String, val args: MutableList<String>, val env: Map<String, String>) {
    fun toCommandLine(): List<String> {
        return listOf(executable, command) + args
    }
}