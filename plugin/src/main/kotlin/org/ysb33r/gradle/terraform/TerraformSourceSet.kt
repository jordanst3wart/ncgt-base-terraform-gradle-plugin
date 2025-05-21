package org.ysb33r.gradle.terraform

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTreeElement
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.ysb33r.gradle.terraform.config.VariableSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import java.io.File
import javax.inject.Inject

/** Describes a Terraform source set
 *
 */
open class TerraformSourceSet @Inject constructor(
    project: Project,
    val name: String,
    val displayName: String
) : PatternFilterable {
    // TODO could change to backendFile...
    // backend text is a bit weird
    val backendText: Property<String> = project.objects.property(String::class.java)
    val srcDir: DirectoryProperty = project.objects.directoryProperty()
    val dataDir: DirectoryProperty = project.objects.directoryProperty()
    val logDir: DirectoryProperty = project.objects.directoryProperty()
    val reportsDir: DirectoryProperty = project.objects.directoryProperty()
    val vars: Variables
    private val patternSet = PatternSet()

    init {
        this.patternSet.include("**/*.tf", "**/*.tfvars", "*.tfstate")

        // TODO test these
        srcDir.set(File("src/${name}/tf"))
        dataDir.set(File(project.layout.buildDirectory.get().asFile,"${name}/tf"))
        logDir.set(File(project.layout.buildDirectory.get().asFile,"${name}/tf/logs"))
        reportsDir.set(File(project.layout.buildDirectory.get().asFile,"${name}/tf/reports"))
        this.vars = Variables(this.srcDir)
    }

    fun setSrcDir(srcDir: String) {
        this.srcDir.set(File(srcDir))
    }

    override fun toString(): String {
        return this.displayName
    }

    fun setBackendText(backText: File) {
        this.backendText.set(backText.readText())
    }

    /** Sets Terraform variables that are applicable to this source set.
     *
     * @param cfg Configurating action.
     *
     */
    fun variables(cfg: Action<VariableSpec>) {
        cfg.execute(this.vars)
    }

    override fun exclude(closure: groovy.lang.Closure<*>): PatternFilterable {
        return patternSet.exclude(closure)
    }

    override fun exclude(spec: Spec<FileTreeElement>): PatternFilterable {
        return patternSet.exclude(spec)
    }

    override fun exclude(vararg strings: String): PatternFilterable {
        return patternSet.exclude(*strings)
    }

    override fun exclude(iterable: Iterable<String>): PatternFilterable {
        return patternSet.exclude(iterable)
    }

    override fun getIncludes(): Set<String> {
        return patternSet.includes
    }

    override fun getExcludes(): Set<String> {
        return patternSet.excludes
    }

    override fun setIncludes(iterable: Iterable<String>): PatternFilterable {
        patternSet.setIncludes(iterable)
        return this
    }

    override fun setExcludes(iterable: Iterable<String>): PatternFilterable {
        patternSet.setExcludes(iterable)
        return this
    }

    override fun include(vararg strings: String): PatternFilterable {
        return patternSet.include(*strings)
    }

    override fun include(iterable: Iterable<String>): PatternFilterable {
        return patternSet.include(iterable)
    }

    override fun include(spec: Spec<FileTreeElement>): PatternFilterable {
        return patternSet.include(spec)
    }

    override fun include(closure: groovy.lang.Closure<*>): PatternFilterable {
        return patternSet.include(closure)
    }
}