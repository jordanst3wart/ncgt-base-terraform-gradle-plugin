package org.ysb33r.gradle.terraform.tasks

import CompileDynamic
import CompileStatic
import Action
import Configuration
import InputFiles
import Nested
import TerraformExecSpec
import Lock
import Source
import TerraformBasePlugin
import AbstractTerraformTask

/** Equivalaent of {@code terraform init}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformInit extends AbstractTerraformTask {

    TerraformInit() {
        super()
        setTerraformCommand('init')
        source = new Source(this)
    }

    // TODO: -backend-configuration

    boolean configureBackend=true

    boolean skipChildModules=false

    boolean verifyPlugins = true

    @Nested
    final Source source

    void source(@DelegatesTo(Source) final Closure cfg)  {
        configureNested(this.source,cfg)
    }

    void source(Action<Source> a)  {
        a.execute(this.getSource())
    }


    final Lock lock = new Lock()

    void lock(@DelegatesTo(Lock) final Closure cfg)  {
        configureNested(this.lock,cfg)
    }

    void lock(Action<Lock> a)  {
        a.execute(this.getLock())
    }

    @InputFiles
    Configuration getPluginConfiguration() {
        switch(this.pluginConfiguration) {
            case Configuration:
                return (Configuration)this.pluginConfiguration
            default:
                return project.configurations.getByName(this.pluginConfiguration.toString())
        }
    }

    @Override
    void exec() {
        copyPlugins()
        super.exec()
    }

    /** Add specific command-line options for the command.
     * If {@code --refresh-dependencies} was specified on the command-line the {@code -upgrade} will be passed
     * to {@code terraform init}.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
//        if(project.gradle.startParameter.refreshDependencies) {
//            execSpec.cmdArgs '-upgrade'
//        }
        if(project.gradle.startParameter.offline) {
            logger.warn "Gradle is running in offline mode. Modules will not be retrieved."
            execSpec.cmdArgs '-get=false'
        }

        execSpec.cmdArgs "-plugin-dir=${getToolExtension().getPluginCacheDir().absolutePath}"
        color(execSpec)
        noInput(execSpec)
        addLockOptionsToCmdLine(execSpec,this.lock)
        addSourceDirToCmdLine(execSpec,this.source)
        execSpec
    }

    @CompileDynamic
    private void copyPlugins() {
        for( File plugin in getPluginConfiguration().files) {
            project.copy {
                from project.zipTree(plugin)
                into getToolExtension().getPluginCacheDir()
            }
        }
    }

    private boolean checkVariables = true
    private Object pluginConfiguration = TerraformBasePlugin.TERRAFORM_CONFIGURATION
}
