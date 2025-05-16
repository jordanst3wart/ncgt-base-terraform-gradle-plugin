pluginManagement {
    resolutionStrategy {
        eachPlugin {
            // get local build version
            // logger.lifecycle(requested.id.name.toString())
            // has to be published to maven local
            /*if (requested.id.id == "foo.bar.terraform") {
                useModule("gradle-terraform-plugin:plugin:FIX-VERSIONS6-SNAPSHOT")
            }*/
        }
    }
    fun getEnvOrThrow(environmentVariable: String): String =
        System.getenv(environmentVariable) ?: throw InvalidUserDataException("Please provide \"$environmentVariable\" environment variable")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

rootProject.name = "gradle-terraform-plugin"
include("plugin")
include(":example")
