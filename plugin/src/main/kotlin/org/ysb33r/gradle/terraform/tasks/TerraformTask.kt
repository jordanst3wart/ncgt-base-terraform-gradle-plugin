package org.ysb33r.gradle.terraform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.ysb33r.gradle.terraform.RunCommand
import org.ysb33r.gradle.terraform.TerraformExecSpec
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.gradle.terraform.TerraformSourceSet
import org.ysb33r.gradle.terraform.config.ConfigExtension
import org.ysb33r.gradle.terraform.config.Json
import org.ysb33r.gradle.terraform.config.Lock
import org.ysb33r.gradle.terraform.config.Parallel
import org.ysb33r.gradle.terraform.internal.Utils
import org.ysb33r.gradle.terraform.internal.Utils.defaultEnvironment
import org.ysb33r.gradle.terraform.internal.Utils.terraformLogFile
import org.ysb33r.grolifant.api.core.ProjectOperations
import java.io.File
import java.util.UUID
import java.util.concurrent.Callable
import javax.inject.Inject

abstract class TerraformTask(): DefaultTask() {
    @Internal
    lateinit var sourceSet: Provider<TerraformSourceSet>

    @Internal
    lateinit var tfCommand: String

    @Internal
    var projectOperations: ProjectOperations = ProjectOperations.find(project)

    @Internal
    var terraformExtension: TerraformExtension = project.extensions.getByType(TerraformExtension::class.java)

    @Internal
    var terraformrc: TerraformRCExtension = TerraformRCExtension.locateTerraformRCExtension(project)

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
        this.tfCommand = cmd
        // not defined at setup time
        // should be a property
        this.sourceSet = project.provider { null } as Provider<TerraformSourceSet>
        withConfigExtensions(configExtensions)
    }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    protected companion object {
        const val NO_COLOR = "-no-color"
        const val JSON_FORMAT = "-json"
    }

    fun setSourceSet(sourceSet: TerraformSourceSet) {
        this.sourceSet = project.providers.provider { sourceSet }
    }

    @TaskAction
    open fun exec() {
        sourceSet.get().logDir.get().asFile.mkdirs()
        terraformLogFile(name, sourceSet.get().logDir).delete()
        Utils.terraformStdErrLogFile(name, sourceSet.get().logDir).delete()
        val execSpec = buildExecSpec()
        execWorkAction(execSpec.getEnvironment() as Map<String, String>, execSpec.getCommandLine() as List<String>)
    }

    private fun execWorkAction(environment: Map<String, String>, commands: List<String>) {
        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(RunCommand::class.java) { parameters ->
            parameters.getCommands().set(commands)
            parameters.getEnvironment().set(environment)
            parameters.getWorkingDir().set(sourceSet.get().srcDir)
            parameters.getStdErrLog().set(Utils.terraformStdErrLogFile(name, sourceSet.get().logDir))
            parameters.getStdOutLog().set(Utils.terraformStdOutLogFile(name, sourceSet.get().logDir))
        }
    }

    protected fun sourceSetVariables(): Provider<List<String>> {
        return project.provider {
            this.sourceSet.get().vars.getCommandLineArgs()
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
            File(sourceSet.get().dataDir.get().asFile, "${sourceSet.get().name}.tf.plan")
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

    protected fun terraformEnvironment(): Map<String, String> {
        val environment = mutableMapOf(
            "TF_DATA_DIR" to sourceSet.get().dataDir.get().asFile.absolutePath,
            "TF_CLI_CONFIG_FILE" to terraformrc.locateTerraformConfigFile().absolutePath,
            "TF_LOG_PATH" to terraformLogFile(name, sourceSet.get().logDir).absolutePath,
            "TF_LOG" to terraformExtension.logLevel.get(),
        )
        environment.putAll(defaultEnvironment())
        environment.putAll(terraformExtension.getEnvironment())
        return environment
    }


    /** Adds a command-line provider.
     */
    protected fun addCommandLineProvider(provider: Provider<List<String>>) {
        this.commandLineProviders.add(provider)
    }

    protected fun buildExecSpec(): TerraformExecSpec {
        val execSpec = TerraformExecSpec(projectOperations, terraformExtension.getResolver())
        execSpec.executable(terraformExtension.resolvableExecutable.executable.absolutePath)
        execSpec.apply {
            command(tfCommand)
            workingDir(sourceSet.get().srcDir)
            environment(terraformEnvironment())
            cmdArgs(defaultCommandParameters)
        }
        addCommandSpecificsToExecSpec(execSpec)
        return execSpec
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
            commandLineProviders.add(project.provider { cex.getCommandLineArgs() })
        }
    }

    /** Add specific command-line options for the command.
     */
    protected open fun addCommandSpecificsToExecSpec(execSpec: TerraformExecSpec): TerraformExecSpec {
        execSpec.cmdArgs(commandLineProviders.map { it.get() }.flatten())
        return execSpec
    }
}