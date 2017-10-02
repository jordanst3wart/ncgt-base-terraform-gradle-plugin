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

import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.testfixtures.ProjectBuilder
import org.ysb33r.gradle.terraform.helpers.DownloadTestSpecification
import spock.lang.IgnoreIf

import java.nio.file.Files


class DownloaderSpec extends DownloadTestSpecification {

    Project project = ProjectBuilder.builder().build()

    @IgnoreIf({ DownloadTestSpecification.SKIP_TESTS })
    def "Download a Packer distribution" () {
        given: "A requirement to download Packer #PACKER_VERSION"
        Downloader dwn = new Downloader(DownloadTestSpecification.PACKER_VERSION,project)
        dwn.downloadRoot = new File(project.buildDir,'download')
        dwn.baseURI = DownloadTestSpecification.PACKER_CACHE_DIR.toURI()

        when: "The distribution root is requested"
        File gotIt = dwn.distributionRoot

        String binaryName = DownloadTestSpecification.OS.windows ? 'packer.exe' : 'packer'

        then: "The distribution is downloaded and unpacked"
        gotIt != null
        new File(gotIt,binaryName).exists()

        and: 'The executable has executable rights'
        Files.isExecutable(gotIt.toPath())

        when: "The executable is run to display the help page"
        OutputStream output = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            executable dwn.packerExecutablePath
            args '--help'
            standardOutput output
        }

        then: "No runtime error is expected"
        result.assertNormalExitValue()

        and: "The expected help information is displayed"
        output.toString().contains('''Available commands are:
    build       build image(s) from template
    fix         fixes templates from old versions of packer
    inspect     see components of a template
    push        push a template and supporting files to a Packer build service
    validate    check that a template is valid
    version     Prints the Packer version''')
    }
}