/*
 * Copyright 2017-2022 the original author or authors.
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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import org.gradle.internal.os.OperatingSystem

import static org.ysb33r.grashicorp.HashicorpUtils.escapedFilePath

class TerraformRCExtensionSpec extends Specification {

    Project project = ProjectBuilder.builder().build()

    def 'Configure terraform rc'() {
        setup:
        def os = OperatingSystem.current()

        when: 'A version is configured'
        project.allprojects {
            apply plugin: 'bot.stewart.terraform'

            // tag::configure-with-tag[]
            terraformrc {
                disableCheckPoint = true // <1>
                disableCheckPointSignature = false // <2>
                useGlobalConfig = false  // <3>

                credentials 'foo.terraform.example', 'foo.terraform.token'  // <4>
            }
            // end::configure-with-tag[]
        }

        def terraformrc = project.extensions.getByType(TerraformRCExtension)
        def hcl = terraformrc.toHCL(new StringWriter()).toString().replaceAll(~/\r?\n/, '!!')

        then:
        terraformrc.pluginCacheDir.get() == new File(project.gradle.gradleUserHomeDir, 'caches/terraform.d')
        hcl == """disable_checkpoint = true
disable_checkpoint_signature = false
plugin_cache_dir = "${escapedFilePath(os, terraformrc.pluginCacheDir.get())}"
plugin_cache_may_break_dependency_lock_file = false
credentials "foo.terraform.example" {
  token = "foo.terraform.token"
}
""".replaceAll(~/\r?\n/, '!!')
    }
}