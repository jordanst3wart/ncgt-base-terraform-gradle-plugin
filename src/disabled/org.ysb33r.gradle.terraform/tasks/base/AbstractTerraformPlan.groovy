package org.ysb33r.gradle.terraform.tasks.base

import Lock
import State
import Variables
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.ysb33r.gradle.terraform.TerraformExecSpec

/** A base class for {@link org.ysb33r.gradle.terraform.tasks.TerraformPlan} and {@link org.ysb33r.gradle.terraform.tasks.TerraformPlanReport}.
 *
 * @since 0.1
 */
@CompileStatic
class AbstractTerraformPlan extends AbstractTerraformTask {

    static final String PLAN_EXT = 'plan'

    static enum PlanPurpose {
        REPORT_PLAN,
        SAVE_PLAN
    }

    static enum PlanType {
        BUILD_PLAN,
        DESTROY_PLAN
    }

    @Nested
    final Lock lock

    void lock(@DelegatesTo(Lock) final Closure cfg) {
        configureNested(this.getLock(), cfg)
    }

    void lock(Action<Lock> a) {
        a.execute(this.getLock())
    }

    @Nested
    final State state

    void state(@DelegatesTo(State) final Closure cfg) {
        configureNested(this.getState(), cfg)
    }

    void state(Action<State> a) {
        a.execute(this.getState())
    }

    @Nested
    final Variables variables

    void variables(@DelegatesTo(Variables) final Closure cfg) {
        configureNested(this.variables, cfg)
    }

    void variables(Action<Variables> a) {
        a.execute(this.variables)
    }

    int getModuleDepth() {
        this.moduleDepth
    }

    void setModuleDepth(int level) {
        this.moduleDepth = level
    }

    void moduleDepth(int level) {
        this.moduleDepth = level
    }

    int getMaxParallel() {
        this.maxParallel
    }

    @Input
    Iterable<String> getResourceTargets() {
        null
    }

    protected AbstractTerraformPlan(PlanPurpose isReport, PlanType type) {
        super()
        setTerraformCommand('plan')
        lock = new Lock()
        state = new State(this)
        planPurpose = isReport
        planType = type
    }

    protected File getReportFile() {
        null
    }

    /** Add specific command-line options for the command.
     *
     * @param execSpec
     * @return execSpec
     */
    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        if (planType == PlanType.DESTROY_PLAN) {
            execSpec.cmdArgs '-destroy'
        }

        color(execSpec)
        noInput(execSpec)
        addMaxParallel(execSpec, getMaxParallel())
        addBooleanCmdLineOption(execSpec, 'refresh', getRefreshMode())
        addModuleDepthToCmdLine(execSpec, getModuleDepth())
        addLockOptionsToCmdLine(execSpec, this.lock)
        addStateOptionsToCmdLine(execSpec, this.state)
        addTargetsToCmdLine(execSpec, getResourceTargets())

        execSpec
    }

    int maxParallel = 0
    int moduleDepth = -1
    boolean refreshMode = true
    final PlanType planType
    final PlanPurpose planPurpose
}
