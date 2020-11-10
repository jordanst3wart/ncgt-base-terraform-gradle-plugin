/*
 * Copyright 2017-2020 the original author or authors.
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
import org.ysb33r.gradle.terraform.TerraformExtension
import org.ysb33r.gradle.terraform.TerraformRCExtension
import org.ysb33r.grolifant.api.OperatingSystem
import org.ysb33r.grolifant.api.v4.wrapper.script.AbstractCacheBinaryTask

@CompileStatic
class TerraformCacheBinary extends AbstractCacheBinaryTask {

    public static final String LOCATION_PROPERTIES_DEFAULT =
        "terraform.properties.${OperatingSystem.current().windows ? 'bat' : 'sh'}"

    TerraformCacheBinary() {
        super(LOCATION_PROPERTIES_DEFAULT)
        this.terraformExtension = project.extensions.getByType(TerraformExtension)
    }

    @Override
    protected String getBinaryVersion() {
        switch (terraformExtension.resolvableExecutableType.type) {
            case 'version':
                return terraformExtension.resolvableExecutableType.value.get()
            default:
                ''
        }
    }

    @Override
    protected String getPropertiesDescription() {
        "Describes the Terraform usage for the ${project.name} project"
    }

    @Override
    protected String getBinaryLocation() {
        terraformExtension.resolvableExecutable.executable.canonicalPath
    }

    @Override
    protected Map<String, String> getAdditionalProperties() {
        TerraformRCExtension terraformrc = project.extensions.getByType(TerraformRCExtension)
        def map = super.additionalProperties
        map.putAll APP_VERSION: binaryVersion,
            APP_LOCATION: binaryLocation,
            USE_GLOBAL_CONFIG: terraformrc.useGlobalConfig.toString(),
            CONFIG_LOCATION: terraformrc.terraformRC.get().absolutePath
        map
    }

    private final TerraformExtension terraformExtension
}
