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
import org.gradle.api.tasks.options.Option
import org.ysb33r.gradle.terraform.TerraformMajorVersion
import org.ysb33r.gradle.terraform.errors.TerraformUpgradeNotSupportedException

/** Equivalent of {@code terraform 0.12upgrade}.
 *
 * @since 0.10.0
 */
@CompileStatic
class TerraformUpgrade extends AbstractTerraformTask {

    TerraformUpgrade() {
        super(null, [], [], null)
        alwaysOutOfDate()
    }

    /** Set auto-approve mode.
     *
     * Once set it cannot be unset for the duration of the Gradle task graph execution.
     *
     * @param state {@code true} will auto-approve upgrade.
     */
    @Option(option = 'approve', description = 'Auto-approve upgrade of sources to Terraform v0.12')
    void setAutoApprove(Boolean state) {
        if (state) {
            supportsYes()
        }
    }

    @Override
    protected String getTerraformCommand() {
        // Look up version
        switch (terraformMajorVersion) {
            case TerraformMajorVersion.VERSION_12:
                return '0.12upgrade'
            case TerraformMajorVersion.VERSION_13:
                return '0.13upgrade'
            default:
                throw new TerraformUpgradeNotSupportedException(
                    'This version of terraform does not have a command to upgrade from a previous version'
                )
        }
    }
}
