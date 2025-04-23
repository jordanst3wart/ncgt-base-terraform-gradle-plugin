package org.ysb33r.gradle.terraform.internal

import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem
import org.ysb33r.gradle.terraform.TerraformRCExtension

@CompileStatic
class Utils {
    public static final OperatingSystem OS = OperatingSystem.current()

    static Map<String, String> awsEnvironment() {
        System.getenv().findAll { k, v -> k.startsWith('AWS_') }
    }

    static Map<String, String> googleEnvironment() {
        System.getenv().findAll { k, v -> k.startsWith('GOOGLE_') }
    }

    static Map<String, String> terraformEnvironment(
        TerraformRCExtension terraformrc,
        String name,
        Provider<File> dataDir,
        Provider<File> logDir,
        String logLevel
    ) {
        def environment = [
            TF_DATA_DIR         : dataDir.get().absolutePath,
            TF_CLI_CONFIG_FILE  : ConfigUtils.locateTerraformConfigFile(terraformrc).absolutePath,
            TF_LOG_PATH         : terraformLogFile(name, logDir).absolutePath,
            TF_LOG              : logLevel ?: '',
        ]
        environment.putAll(defaultEnvironment())
        environment
    }

    static File terraformLogFile(String name, Provider<File> logDir) {
        new File(logDir.get(), "${name}.log").absoluteFile
    }

    private static Map<String, String> defaultEnvironment() {
        if (OS.windows) {
            [
                TEMP        : System.getenv('TEMP'),
                TMP         : System.getenv('TMP'),
                HOMEDRIVE   : System.getenv('HOMEDRIVE'),
                HOMEPATH    : System.getenv('HOMEPATH'),
                USERPROFILE : System.getenv('USERPROFILE'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ]
        } else {
            [
                HOME        : System.getProperty('user.home'),
                (OS.pathVar): System.getenv(OS.pathVar)
            ]
        }
    }
}
