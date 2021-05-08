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
package org.ysb33r.gradle.terraform.tasks

import groovy.transform.CompileStatic
import org.gradle.api.Transformer
import org.gradle.api.provider.Provider
import org.ysb33r.gradle.terraform.TerraformExecSpec

/**
 * Generates a destroy plan
 *
 * @author Schalk W. Cronjé
 */
@CompileStatic
class TerraformDestroyPlan extends TerraformPlan {
    @Override
    Provider<File> getPlanOutputFile() {
        dataDir.map({ File reportDir ->
            new File(reportDir, "${sourceSet.name}.tf.destroy.plan")
        } as Transformer<File, File>)
    }

    @Override
    Provider<File> getPlanReportOutputFile() {
        reportsDir.map({ File reportDir ->
            new File(reportDir, "${sourceSet.name}.tf.destroy.plan.${jsonReport ? 'json' : 'txt'}")
        } as Transformer<File, File>)
    }

    @Override
    protected TerraformExecSpec addCommandSpecificsToExecSpec(TerraformExecSpec execSpec) {
        super.addCommandSpecificsToExecSpec(execSpec)
        execSpec.cmdArgs '-destroy'
        execSpec
    }
}
