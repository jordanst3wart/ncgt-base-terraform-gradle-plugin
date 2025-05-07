package org.ysb33r.gradle.terraform.config.multilevel

import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.config.VariableSpec
import org.ysb33r.gradle.terraform.errors.ConfigurationException
import java.io.File

class Variables(
    private val rootDirResolver: Provider<File>
) : VariableSpec {

    val name: String = "variables"
    private val varsFilesPair: VarsFilesPair = VarsFilesPair()

    override fun `var`(name: String, value: Any) {
        varsFilesPair.vars[name] = value
    }

    override fun map(name: String, map: Map<String, *>) {
        varsFilesPair.vars[name] = map
    }

    override fun list(name: String, val1: Any, vararg vals: Any) {
        val inputs = mutableListOf<Any>(val1)
        inputs.addAll(vals)
        varsFilesPair.vars[name] = inputs
    }

    override fun list(name: String, vals: Iterable<*>) {
        varsFilesPair.vars[name] = vals.toList()
    }

    override fun file(fileName: String) {
        varsFilesPair.files.add(fileName)
    }

    override fun getCommandLineArgs(): List<String> {
        val root = rootDirResolver.orNull?.toPath()
            ?: throw ConfigurationException(
                "This method can only be called when attached to a task extension or a source set"
            )

        return this.varsFilesPair.commandLineArgs(root)
    }

    override fun toString(): String {
        return "Terraform variables: ${this.varsFilesPair}"
    }

    val allVars: VarsFilesPair
        get() = this.varsFilesPair
}