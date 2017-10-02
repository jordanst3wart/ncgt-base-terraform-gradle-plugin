package org.ysb33r.grashicorp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.cyberneko.html.parsers.SAXParser
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.util.GradleVersion
import org.ysb33r.grolifant.api.OperatingSystem

import static org.ysb33r.grolifant.api.OperatingSystem.Arch.X86
import static org.ysb33r.grolifant.api.OperatingSystem.Arch.X86_64

/** Code for dealing with Hashicorp standards.
 *
 * @since 0.1
 */
@CompileStatic
class HashicorpUtils {

    /** Get the download URI for Hashicorp Releases. Sprecifiy a product to get the specific URI.
     *
     * <p> Code will check for the existence of a System property  {@code org.ysb33r.gradle.<NAM>.uri} or
     * {@code org.ysb33r.gradle.hashicorp.releases.uri} before returning the default.
     *
     * @param name Name of product or package. Can be null or empty to get baseURI for Hashicorp releases
     *
     * @return
     */
    static String getDownloadBaseUri(final String name) {
        if(name == null || name.empty) {
            System.getProperty('org.ysb33r.gradle.hashicorp.releases.uri') ?: 'https://releases.hashicorp.com'
        } else {
            System.getProperty("org.ysb33r.gradle.${name}.uri") ?: "${getDownloadBaseUri(null)}/${name}"
        }
    }

    /** Returns a formatted string that can be used in file names and URIs.
     *
     * <p> Does not support ARM at present.
     *
     * @param os Operating system
     * @return String depicting operating systems and architecture in a Hashicorp standard.
     *   Can be {@code null} is setup is not supported.
     */
    static String osArch(final OperatingSystem os) {
        final OperatingSystem.Arch arch = os.getArch()
        String variant
        String osname
        if(os.windows) {
            osname = 'windows'
            variant = (os.arch == X86) ? '386' : 'amd64'
        } else if(os.linux) {
            osname = 'linux'
            switch(arch) {
                case X86_64:
                    variant = 'amd64'
                    break
                case X86:
                    variant = '386'
                    break
            }
        } else if(os.macOsX) {
            osname = 'darwin'
            variant = 'amd64'
        }  else if(os.solaris) {
            osname = 'solaris'
            variant = 'amd64'
        } else if(os.freeBSD) {
            osname = 'freebsd'
            switch(arch) {
                case X86_64:
                    variant = 'amd64'
                    break
                case X86:
                    variant = '386'
                    break
            }
        }
        variant ? "${osname}_${variant}" : null
    }

    /** Obtains the latest version of a Terraform provider
     *
     * @param provider
     * @return Latest version or {@code null}
     */
    @Nullable
    static String getLatestTerraformProviderVersion(final String provider) {

        final String name = "terraform-provider-${provider}"
        final String host = getDownloadBaseUri(name)
        if(host.startsWith('file:')) {
            getLatestVersionFileSystem(host,name)
        } else if(host.startsWith('http')) {
            getLatestVersionHttp('terraform-gradle-plugin',host,name)
        } else {
            throw new GradleException("${host} is not a supported URI")
        }
    }

    // Assumes physical layout on disk as per Hashicorp cloud.
    // If the cloud path was https://releases.hashicorp.com/terraform-provider-aws/0.1.0/terraform-provider-aws_0.1.0_darwin_amd64.zip,
    // then expect the host to point to a directory above terraform-provider-aws
    private static getLatestVersionFileSystem(final String host,final String name) {
        File baseDir = new File(host.toURI())
        final List<String> versions = baseDir.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                pathname.isDirectory()
            }
        }).collect { File dirname ->
            dirname.name
        }.sort { String a,String b ->
            GradleVersion.version(b) <=> GradleVersion.version(a)
        }
        versions.empty ? null : versions[0]
    }

    @CompileDynamic
    private static getLatestVersionHttp(final String agentName,final String host,final String name) {
        SAXParser parser = new SAXParser()
        def page = new XmlSlurper(parser).parseText(host.toURL().getText(requestProperties: ['User-Agent': agentName]))
        List<String> versions = page.depthFirst().findAll {
            it.@href.toString().startsWith("/${name}")
        }.collect {
            it.toString().replace("${name}_",'')
        }.sort { a,b ->
            GradleVersion.version(b) <=> GradleVersion.version(a)
        }
        versions.empty ? null : versions[0]
    }
}
