package org.ysb33r.gradle.terraform.tasks

import CompileStatic
import OutputFile
import TerraformExecSpec
import AbstractTerraformPlan

import static AbstractTerraformPlan.PlanPurpose.SAVE_PLAN
import static AbstractTerraformPlan.PlanType.BUILD_PLAN

/** Equivalent of {@code terraform validate}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformPlan extends AbstractTerraformPlan {

    TerraformPlan() {
        super(SAVE_PLAN,BUILD_PLAN)
//        variables = new Variables(this)
//        source = new Source(this)
    }

    @OutputFile
    File getPlanFile() {
        null
    }

//    @Input
//    boolean getCheckVariables() {
//        this.checkVariables
//    }
//
//    void setCheckVariables(boolean v_) {
//        this.checkVariables = v_
    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        execSpec.cmdArgs "-out=${getPlanFile().absolutePath}"
        super.addCommandSpecificsToExecSpec(execSpec)
    }
//    }
//
//    void checkVariables(boolean v_) {
//        this.checkVariables = v_
//    }
//
//    void source(@DelegatesTo(Source) final Closure cfg)  {
//        configureNested(this.source,cfg)
//    }
//
//    void source(Action<Source> a)  {
//        a.execute(this.getSource())
//    }
//
//    @Nested
//    final Source source
//

//    /** Add specific command-line options for the command.
//     *
//     * @param execSpec
//     * @return execSpec
//     */
//    @Override
//    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
//        color(execSpec)
//        addBooleanCmdLineOption(execSpec,'check-variables',getCheckVariables())
//        addVariablesToCmdLine(execSpec,variables.getVars())
//        addVariableFilesToCmdLine(execSpec,variables.getFiles())
//        addSourceDirToCmdLine(execSpec,this.source)
//
//project.logger.lifecycle(execSpec.environment.toString())
//project.logger.lifecycle(execSpec.commandLine.toString())
//        execSpec
//    }
//
//    private boolean checkVariables = true
}
