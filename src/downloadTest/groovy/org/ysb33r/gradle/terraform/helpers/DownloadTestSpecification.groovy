//
// ============================================================================
// (C) Copyright Schalk W. Cronje 2017
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
// ============================================================================
//

package org.ysb33r.gradle.terraform.helpers

import org.ysb33r.gradle.olifant.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformExtension
import spock.lang.Specification


class DownloadTestSpecification extends Specification {

    static final String PACKER_VERSION = System.getProperty('PACKER_VERSION') ?: TerraformExtension.TERRAFORM_DEFAULT
    static final File PACKER_CACHE_DIR = new File( System.getProperty('PACKER_CACHE_DIR') ?: './build/packer-binaries').absoluteFile
    static final File RESOURCES_DIR = new File (System.getProperty('RESOURCES_DIR') ?: './src/downloadTest/resources')

    static final OperatingSystem OS = OperatingSystem.current()
    static final boolean SKIP_TESTS = !(OS.isMacOsX() || OS.isLinux() || OS.isWindows() || OS.isFreeBSD())

}