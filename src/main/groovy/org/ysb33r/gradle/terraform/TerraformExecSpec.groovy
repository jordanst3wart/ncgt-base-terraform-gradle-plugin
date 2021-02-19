/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.exec.AbstractCommandExecSpec
import org.ysb33r.grolifant.api.v4.exec.ExternalExecutable

/** An execution specification for {@code Terraform}.
 *
 * @since 0.1
 */
@CompileStatic
class TerraformExecSpec extends AbstractCommandExecSpec {
    /** Construct class and attach it to specific project.
     *
     * @param project Project this exec spec is attached.
     * @deprecated
     */
    @Deprecated
    TerraformExecSpec(Project project, ExternalExecutable resolver) {
        super(ProjectOperations.find(project), resolver)
    }

    /** Construct class and attach it to specific project.
     *
     * @param projectOperations Project this exec spec is attached.
     *
     * @since 0.10.0
     */
    TerraformExecSpec(ProjectOperations projectOperations, ExternalExecutable resolver) {
        super(projectOperations, resolver)
    }
}
