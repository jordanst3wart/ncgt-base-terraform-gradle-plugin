package org.ysb33r.gradle.terraform

import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.AbstractCommandExecSpec
import org.ysb33r.grolifant.api.v4.exec.ExternalExecutable

/** An execution specification for [Terraform].
 *
 */
class TerraformExecSpec(
    projectOperations: ProjectOperations,
    resolver: ExternalExecutable
) : AbstractCommandExecSpec(projectOperations, resolver)