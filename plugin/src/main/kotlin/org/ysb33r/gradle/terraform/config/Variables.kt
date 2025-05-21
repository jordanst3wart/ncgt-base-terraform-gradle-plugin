package org.ysb33r.gradle.terraform.config

import org.gradle.api.file.DirectoryProperty
import org.ysb33r.gradle.terraform.errors.ConfigurationException
import java.nio.file.Path
import kotlin.streams.toList

class Variables(
    private val directory: DirectoryProperty
) : VariableSpec {
    val name: String = "variables"
    val vars: MutableMap<String, Any> = mutableMapOf()
    val files: MutableList<String> = mutableListOf()

    fun commandLineArgs(root: Path): List<String> {
        val varList: MutableList<String> = mutableListOf()
        varList.addAll(fileNames.stream().map { fileName: String ->
            "-var-file=${root.resolve(fileName).toFile().absolutePath}"
        }.toList())
        return varList
    }

    val fileNames: Set<String>
        get() = this.files.toSet()

    override fun `var`(name: String, value: Any) {
        vars[name] = value
    }

    override fun map(name: String, map: Map<String, *>) {
        vars[name] = map
    }

    override fun list(name: String, val1: Any, vararg vals: Any) {
        val inputs = mutableListOf<Any>(val1)
        inputs.addAll(vals)
        vars[name] = inputs
    }

    override fun list(name: String, vals: Iterable<*>) {
        vars[name] = vals.toList()
    }

    override fun file(fileName: String) {
        files.add(fileName)
    }

    override fun getCommandLineArgs(): List<String> {
        val root = directory.get().asFile.toPath()
            ?: throw ConfigurationException(
                "This method can only be called when attached to a task extension or a source set"
            )

        return this.commandLineArgs(root)
    }
}