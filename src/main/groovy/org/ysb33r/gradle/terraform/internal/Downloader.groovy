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

package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.ysb33r.gradle.olifant.AbstractDistributionInstaller
import org.ysb33r.gradle.olifant.OperatingSystem
import org.ysb33r.gradle.olifant.errors.DistributionFailedException

import static org.ysb33r.gradle.olifant.OperatingSystem.Arch.X86
import static org.ysb33r.gradle.olifant.OperatingSystem.Arch.X86_64

/** Downloads specific versions of {@code Terraform}.
 *
 * <p> Currently limited to Windows (x86, x86_64), MacOS, Linux (x86, x86_64), Solaris (x86_64) and FreeBSD (x86, x86_64).
 * 
 * <p> There are more
 * binary packages are available from the Terraform site, but currently these are not being tested not implemented.
 * This includes:
 * 
 * <ul>
 *    <li> linux_arm.zip
 *    <li> freebsd_arm.zip
 *    <li> openbsd_386, openbsd_amd64
 * </ul>
 * <p> (Patches welcome!)
 */
@CompileStatic
class Downloader extends AbstractDistributionInstaller {
    static final OperatingSystem OS = OperatingSystem.current()
    static final OperatingSystem.Arch ARCH = OS.getArch()

    static String baseURI = System.getProperty('org.ysb33r.gradle.terraform.uri') ?: 'https://releases.hashicorp.com/terraform'

    /** Creates a downloader
     *
     * @param version Version of {@code Terraform}.
     * @param project Project this is associated with.
     */
    Downloader(final String version, final Project project) {
        super('terraform',version,'native-binaries/terraform',project)
    }

    /** Tells the system whether downloading can be supported.
     *
     * @return {@b true} for supported platforms,
     */
    static boolean isDownloadSupported() {
        (OS.windows || OS.linux || OS.macOsX || OS.freeBSD) && (OS.arch == X86 || OS.arch == X86_64)
    }

    /** Provides an appropriate URI to download a specific version of Terraform.
     *
     * @param ver Version of Terraform to download
     * @return URI for a supported platform; {@code null} otherwise.
     */
    @Override
    URI uriFromVersion(final String ver) {
        String variant
        String osname
        if(OS.windows) {
            osname = 'windows'
            variant = (OS.arch == X86) ? '386' : 'amd64'
        } else if(OS.linux) {
            osname = 'linux'
            switch(ARCH) {
                case X86_64:
                    variant = 'amd64'
                    break
                case X86:
                    variant = '386'
                    break
            }
        } else if(OS.macOsX) {
            osname = 'darwin'
            variant = 'amd64'
        }  else if(OS.solaris) {
            osname = 'solaris'
            variant = 'amd64'
        } else if(OS.freeBSD) {
            osname = 'freebsd'
            switch(ARCH) {
                case X86_64:
                    variant = 'amd64'
                    break
                case X86:
                    variant = '386'
                    break
            }
        }
        variant ? "${baseURI}/${ver}/terraform_${ver}_${osname}_${variant}.zip".toURI() : null
    }

    /** Returns the path to the {@code terraform} executable.
     * Will force a download if not already downloaded.
     *
     * @return Location of {@code terraform} or null if not a supported operating system.
     */
    File getTerraformExecutablePath() {
        File root = getDistributionRoot()
        if(root == null) {
            return null
        } else {
            new File(root,exeName)
        }
    }

    /** Validates that the unpacked distribution is good.
     *
     * @param distDir Directory where distribution was unpacked to.
     * @param distributionDescription A descriptive name of the distribution
     * @return {@code distDir} as {@code Packer} distributions contains only a single executable.
     *
     * @throw {@link org.ysb33r.gradle.olifant.DistributionFailedException} if distribution failed to meet criteria.
     */
    @Override
    protected File getAndVerifyDistributionRoot(File distDir, String distributionDescription) {

        File checkFor = new File(distDir,exeName)

        if(!checkFor.exists()) {
            throw new DistributionFailedException("${checkFor.name} not found in downloaded ${distributionDescription} distribution.")
        }

        distDir
    }

    private String getExeName() {
        OS.windows ? 'terraform.exe' : 'terraform'
    }
}

