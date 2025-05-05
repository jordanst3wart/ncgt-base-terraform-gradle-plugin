package org.ysb33r.gradle.terraform

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.ysb33r.gradle.terraform.config.VariableSpec
import org.ysb33r.gradle.terraform.config.multilevel.Variables
import org.ysb33r.grolifant.api.core.ProjectOperations
import java.io.File
import javax.inject.Inject

/** Describes a Terraform source set
 *
 */
class TerraformSourceDirectorySet @Inject constructor(
    project: Project,
    val name: String,
    val displayName: String
) : PatternFilterable {

    private val backendText: Property<String>
    private val sourceDir: Property<File>
    val dataDir: Property<File>
    val logDir: Property<File>
    val reportsDir: Property<File>
    private val projectOperations: ProjectOperations
    private val vars: Variables
    private val patternSet = PatternSet()
    private val secondarySources: MutableList<Any>
    private val secondarySourcesProvider: Provider<List<File>>

    init {
        this.projectOperations = ProjectOperations.maybeCreateExtension(project)
        this.patternSet.include("**/*.tf", "**/*.tfvars", "*.tfstate")

        sourceDir = project.objects.property(File::class.java)
        dataDir = project.objects.property(File::class.java)
        logDir = project.objects.property(File::class.java)
        reportsDir = project.objects.property(File::class.java)
        backendText = project.objects.property(String::class.java)

        projectOperations.fsOperations.updateFileProperty(
            sourceDir,
            "src/${name}/tf"
        )

        projectOperations.fsOperations.updateFileProperty(
            dataDir,
            projectOperations.buildDirDescendant("${name}/tf")
        )

        projectOperations.fsOperations.updateFileProperty(
            logDir,
            projectOperations.buildDirDescendant("${name}/tf/logs")
        )

        projectOperations.fsOperations.updateFileProperty(
            reportsDir,
            projectOperations.buildDirDescendant("${name}/tf/reports")
        )

        this.vars = Variables(this.sourceDir)
        this.secondarySources = mutableListOf()
        this.secondarySourcesProvider = projectOperations.provider {
            projectOperations.fsOperations.files(secondarySources).toList()
        }
    }

    override fun toString(): String {
        return this.displayName
    }

    fun getSrcDir(): Provider<File> {
        return this.sourceDir
    }

    /** Sets the source directory.
     *
     * @param dir Directory can be anything convertible using [Project.file].
     */
    fun setSrcDir(dir: Any) {
        projectOperations.fsOperations.updateFileProperty(this.sourceDir, dir)
    }

    fun setBackendText(backText: String) {
        this.backendText.set(backText)
    }

    fun backendPropertyText(): Property<String> {
        return backendText
    }

    /** Data directory provider.
     *
     * @return File provider.
     */
    fun getDataDir(): Provider<File> {
        return this.dataDir
    }

    /** Log directory provider.
     *
     * @return File provider.
     */
    fun getLogDir(): Provider<File> {
        return this.logDir
    }

    /** Reports directory.
     *
     * @return File provider.
     */
    fun getReportsDir(): Provider<File> {
        return this.reportsDir
    }

    /**
     * Additional sources that affects infrastructure.
     *
     * @param files Anything convertible to a file.
     *
     * @since 0.10.
     */
    fun secondarySources(vararg files: Any) {
        this.secondarySources.addAll(files)
    }

    /**
     * Additional sources that affects infrastructure.
     *
     * @param files Anything convertible to a file.
     *
     * @since 0.10.
     */
    fun secondarySources(files: Iterable<Any>) {
        this.secondarySources.addAll(files)
    }

    /** Provides a list of secondary sources.
     *
     * @return Provider never returns null, but could return an empty list.
     */
    fun getSecondarySources(): Provider<List<File>> {
        return this.secondarySourcesProvider
    }

    /** Sets Terraform variables that are applicable to this source set.
     *
     * @param cfg Configurating action.
     *
     * @since 0.2
     */
    fun variables(cfg: Action<VariableSpec>) {
        cfg.execute(this.vars)
    }

    /** Get all terraform variables applicable to this source set.
     *
     */
    fun getVariables(): VariableSpec {
        return this.vars
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