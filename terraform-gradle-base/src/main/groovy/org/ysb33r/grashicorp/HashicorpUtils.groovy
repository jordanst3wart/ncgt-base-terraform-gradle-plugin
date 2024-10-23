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
package org.ysb33r.grashicorp

import groovy.transform.CompileStatic
import org.ysb33r.grolifant.api.core.OperatingSystem

import java.util.regex.Pattern

import static org.ysb33r.grolifant.api.core.OperatingSystem.Arch.ARM64
import static org.ysb33r.grolifant.api.core.OperatingSystem.Arch.X86
import static org.ysb33r.grolifant.api.core.OperatingSystem.Arch.X86_64

/** Code for dealing with Hashicorp standards.
 *
 * @since 0.1
 */
@CompileStatic
@SuppressWarnings('LineLength')
class HashicorpUtils {

    /** Get the download URI for Hashicorp Releases. Specify a product to get the specific URI.
     *
     * <p> Code will check for the existence of a System property  {@code org.ysb33r.gradle.<NAM>  .uri} or
     * {@code org.ysb33r.gradle.hashicorp.releases.uri} before returning the default.
     *
     * @param name Name of product or package. Can be null or empty to get baseURI for Hashicorp releases
     *
     * @return
     */
    static String getDownloadBaseUri(final String name) {
        if (name == null || name.empty) {
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
        final OperatingSystem.Arch arch = os.arch
        String variant
        String osname
        if (os.windows) {
            osname = 'windows'
            variant = (os.arch == X86) ? '386' : 'amd64'
        } else if (os.linux) {
            osname = 'linux'
            switch (arch) {
                case X86_64:
                    variant = VARIANT_64BIT
                    break
                case X86:
                    variant = VARIANT_32BIT
                    break
                case ARM64:
                    variant = VARIANT_ARM64
                    break
            }
        } else if (os.macOsX) {
            osname = 'darwin'
            switch (arch) {
                case X86_64:
                    variant = VARIANT_64BIT
                    break
                case ARM64:
                    variant = VARIANT_ARM64
                    break
            }
        } else if (os.solaris) {
            osname = 'solaris'
            variant = VARIANT_64BIT
        } else if (os.freeBSD) {
            osname = 'freebsd'
            switch (arch) {
                case X86_64:
                    variant = VARIANT_64BIT
                    break
                case X86:
                    variant = VARIANT_32BIT
                    break
            }
        }
        variant ? "${osname}_${variant}" : null
    }

    /** Escapes file paths for safe inclusion in HCL files.
     *
     * @param os Operating system to apply this to,
     * @param path File path to escape
     * @return Escape file path as a string.
     */
    static String escapedFilePath(OperatingSystem os, File path) {
        os.windows ? path.absolutePath.replaceAll(BACKSLASH, DOUBLE_BACKSLASH) : path.absolutePath
    }

    private final static String VARIANT_32BIT = '386'
    private final static String VARIANT_64BIT = 'amd64'
    private final static String VARIANT_ARM64 = 'arm64'
    private final static Pattern BACKSLASH = ~/\x5C/
    private final static String DOUBLE_BACKSLASH = '\\\\\\\\'
}
