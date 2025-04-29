package org.ysb33r.gradle.terraform.config.multilevel

import java.nio.file.Path
import kotlin.streams.toList

class VarsFilesPair {
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

    override fun toString(): String {
        return "vars=${vars}, files=${files}"
    }
}