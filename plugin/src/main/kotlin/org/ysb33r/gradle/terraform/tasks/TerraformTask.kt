package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.ysb33r.gradle.terraform.RunCommand
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceDirectorySet
import org.ysb33r.gradle.terraform.config.ConfigExtension
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.internal.ConfigUtils
import org.ysb33r.gradle.terraform.internal.Utils
import org.ysb33r.grolifant.api.core.ProjectOperations
import java.io.File
import java.util.UUID
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class TerraformTask(): DefaultTask() {
    @Internal
    lateinit var sourceSet: Provider<TerraformSourceDirectorySet>

    @Internal
    lateinit var command: String

    @Internal
    var projectOperations: ProjectOperations = ProjectOperations.find(project)

    @Internal
    var terraformExtension: TerraformExtension = project.extensions.getByType(TerraformExtension::class.java)

    @Internal
    var terraformrc: TerraformRCExtension = ConfigUtils.locateTerraformRCExtension(project)

    @Internal
    val commandLineProviders: MutableList<Provider<List<String>>> = mutableListOf()

    @Internal
    val tfVarProviders: MutableList<Provider<List<String>>> = mutableListOf()

    @Internal
    val defaultCommandParameters: MutableList<String> = mutableListOf()

    /**
     * @param cmd Command to be executed. See https://www.terraform.io/docs/commands/index.html for details.
     * @param configExtensions Configuration extensions to be added to this task.
     */
    protected constructor(
        cmd: String,
        configExtensions: List<Class<out ConfigExtension>>
    ) : this() {
        this.command = cmd
        // not defined at setup time
        this.sourceSet = project.provider { null } as Provider<TerraformSourceDirectorySet>
        withConfigExtensions(configExtensions)
    }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    protected companion object {
        const val NO_COLOR = "-no-color"
        const val JSON_FORMAT = "-json"
    }

    fun setSourceSet(sourceSet: TerraformSourceDirectorySet) {
        this.sourceSet = project.providers.provider { sourceSet }
    }

    @TaskAction
    open fun exec() {
        sourceSet.get().logDir.get().mkdirs()
        Utils.terraformLogFile(name, sourceSet.get().logDir).delete()
        Utils.terraformErrorLogFile(name, sourceSet.get().logDir).delete()
        val execSpec = buildExecSpec()
        // might not need captureStdout
        execWorkAction(execSpec.getEnvironment() as Map<String, String>, execSpec.getCommandLine() as List<String>)
    }

    private fun execWorkAction(environment: Map<String, String>, commands: List<String>) {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(RunCommand::class.java) { parameters ->
            parameters.getCommands().set(commands)
            parameters.getEnvironment().set(environment)
            parameters.getWorkingDir().set(sourceSet.get().getSrcDir())
            parameters.getErrorLog().set(Utils.terraformErrorLogFile(name, sourceSet.get().logDir))
        }
    }

    protected fun sourceSetVariables(): Provider<List<String>> {
        return project.provider {
            this.sourceSet.get().getVariables().getCommandLineArgs()
        }
    }

    /**
     * Marks task to always be out of date.
     */
    protected fun alwaysOutOfDate() {
        inputs.property("always-out-of-date", UUID.randomUUID().toString())
    }

    @get:Internal
    protected val planFile: Provider<File>
        get() = project.provider(Callable<File> {
            File(sourceSet.get().dataDir.get(), "${sourceSet.get().name}.tf.plan")
        })

    /**
     * To be called from tasks where the command supports [input].
     */
    protected fun supportsInputs() {
        defaultCommandParameters.add("-input=false")
    }

    /**
     * To be called from tasks where the command supports [auto-approve].
     */
    protected fun supportsAutoApprove() {
        defaultCommandParameters.add("-auto-approve")
    }

    /** To be called from tasks where the command supports [no-color].
     *
     * Will get set if [--console=plain] was provided to Gradle
     *
     * @param withColor If set to [false], the task will always run without color output.
     */
    protected fun supportsColor(withColor: Boolean = true) {
        val mode = projectOperations.consoleOutput
        if (mode == ConsoleOutput.Plain ||
            (mode == ConsoleOutput.Auto && System.getenv("TERM") == "dumb") ||
            !withColor
        ) {
            defaultCommandParameters.add(NO_COLOR)
        }
    }

    @get:Input
    protected val terraformEnvironment: Map<String, String>
        get() = Utils.terraformEnvironment(
            terraformrc,
            name,
            sourceSet.get().dataDir,
            sourceSet.get().logDir,
            terraformExtension.logLevel.get()
        )

    /** Adds a command-line provider.
     *
     * @param provider
     */
    protected fun addCommandLineProvider(provider: Provider<List<String>>) {
        this.commandLineProviders.add(provider)
    }

    protected fun buildExecSpec(): TerraformExecSpec {
        val execSpec = createExecSpec()
        addExecutableToExecSpec(execSpec)
        return configureExecSpec(execSpec)
    }

    protected fun addExecutableToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        execSpec.executable(terraformExtension.resolvableExecutable.executable.absolutePath)
        return execSpec
    }

    /** Configures a [TerraformExecSpec].
     *
     * @param execSpec Specification to be configured
     * @return Configured specification
     */
    protected fun configureExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        configureExecSpecForCmd(execSpec, command, defaultCommandParameters)
        addCommandSpecificsToExecSpec(execSpec)
        return execSpec
    }

    /** Configures execution specification for a specific command.
     *
     * @param execSpec Specification to configure.
     * @param tfcmd Terraform command.
     * @param cmdParams Default command parameters.
     * @return Configures specification.
     */
    protected fun configureExecSpecForCmd(
        execSpec: TerraformExecSpec,
        tfcmd: String,
        cmdParams: List<String>
    ): TerraformExecSpec {
        val tfEnv = this.terraformEnvironment
        execSpec.apply {
            command(tfcmd)
            workingDir(sourceSet.get().getSrcDir())
            environment(tfEnv)
            cmdArgs(cmdParams)
        }
        return execSpec
    }

    protected fun createExecSpec(): TerraformExecSpec {
        return TerraformExecSpec(projectOperations, terraformExtension.getResolver())
    }

    /** To be called subclass constructor for defining specific configuration extensions that are
     * supported.
     */
    private fun withConfigExtensions(configExtensions: List<Class<out ConfigExtension>>) {
        for (it in configExtensions) {
            val cex: ConfigExtension = when (it) {
                Lock::class.java -> terraformExtension.lock
                Parallel::class.java -> terraformExtension.parallel
                Json::class.java -> terraformExtension.json
                else -> project.objects.newInstance(it)
            }
            extensions.add(cex.name, cex)
            commandLineProviders.add(projectOperations.provider { cex.getCommandLineArgs() })
        }
    }

    /** Add specific command-line options for the command.
     */
    protected open fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        execSpec.cmdArgs(commandLineProviders.map { it.get() }.flatten())
        return execSpec
    }
}