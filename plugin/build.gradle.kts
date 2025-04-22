import java.text.SimpleDateFormat
import java.util.Date

plugins {
    groovy
    codenarc
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.grgit)
}

apply(from = rootProject.file("gradle/plugin-dev.gradle"))
apply(from = rootProject.file("gradle/codenarc.gradle"))



fun Map<String, Any?>.getOrSystemEnvOrDefault(key: String, defaultValue: String): String = this.getOrElse(key) {
    System.getenv().getOrElse(key) {
        logger.warn("'$key' property is not defined, defaulting to '$defaultValue'")
        defaultValue
    }
}.toString()

val githubUsername = project.properties.getOrSystemEnvOrDefault("GH_USERNAME", "newscorp-ghfb")
/*
 * this is a github classic token that requires publishing permissions to write a new package
 */
val githubToken = project.properties.getOrSystemEnvOrDefault("GH_PACKAGES_TOKEN", "dummyToken")

fun String.formatBranchName(): String {
    // Defines the regex pattern for special characters
    val specialCharsRegex = Regex("[@!#\$%^&*()\\[\\]{}|\\\\/:;\"'<>, ]")
    return this.replace(specialCharsRegex, "-").uppercase()
}

fun createVersion(): String {
    val gitShortHash = grgit.head().abbreviatedId
    val dateTag = SimpleDateFormat("yyyyMMdd").format(Date())
    return if (grgit.branch.current().name == "main") {
        "$dateTag-$gitShortHash"
    } else {
        "${grgit.branch.current().name.formatBranchName()}-SNAPSHOT"
    }
}

// create a gradle plugin uploading to gradle plugins website
gradlePlugin {
    plugins {
        create("terraformPlugin") {
            id = "foo.bar.terraform" // property("ID").toString()
            implementationClass = "org.ysb33r.gradle.terraform.plugins.TerraformPlugin" // property("IMPLEMENTATION_CLASS").toString()
            version = createVersion()
            displayName = "Terraform Plugin"
            description = "Provides Terraform extension and tasks. No need to have terraform installed as plugin will take care of caching and installation in a similar fashion as to have Gradle distributions are cached"
            tags = listOf("terraform")
            // website = property("WEBSITE").toString()
            // vcsUrl = property("VCS_URL").toString()
        }
        /*
                terraformBasePlugin {
            id = 'foo.bar.terraform.base'
            displayName = 'Terraform Base plugin'
            description = 'Provides Terraform extension and tasks. No need to have terraform installed as plugin will take care of caching and installation in a similar fashion as to have Gradle distributions are cached'
            group = 'foo.bar.gradle.terraform'
        }
        terraformPlugin {
            id = 'foo.bar.terraform'
            displayName = 'Terraform conventions plugin'
            description = 'Provides Terraform conventions, source sets and predefined tasks'
            group = 'foo.bar.gradle.terraform'
        }
        terraformRCPlugin {
            id = 'foo.bar.terraform.rc'
            displayName = 'Terraform Configuration (terraformrc) plugin'
            description = 'Deals specifically with the creation of terraformrc files'
            group = 'foo.bar.gradle.terraform'
        }
         */
    }
}


publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            credentials {
                username = githubUsername
                password = githubToken
            }
            uri("https://maven.pkg.github.com/jordanst3wart/gradle-terraform-plugin")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED")
}

