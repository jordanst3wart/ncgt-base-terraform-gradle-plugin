package org.ysb33r.gradle.terraform.config

interface VariableSpec {
    fun `var`(name: String, value: Any)
    fun map(name: String, map: Map<String, *>)
    fun list(name: String, val1: Any, vararg vals: Any)
    fun list(name: String, vals: Iterable<*>)
    fun file(fileName: Any)
    val commandLineArgs: List<String>
}