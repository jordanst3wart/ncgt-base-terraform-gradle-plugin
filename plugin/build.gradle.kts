import java.text.SimpleDateFormat
import java.util.Date

plugins {
    groovy
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.grgit)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    api("org.ysb33r.gradle:grolifant-herd:3.0.1")

    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation(localGroovy())
    testImplementation(gradleTestKit())
    testImplementation("org.ysb33r.gradle:grolifant-herd:3.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

fun Map<String, Any?>.getOrSystemEnvOrDefault(key: String, defaultValue: String): String = this.getOrElse(key) {
    System.getenv().getOrElse(key) {
        logger.warn("'$key' property is not defined, defaulting to '$defaultValue'")
        defaultValue
    }
}.toString()

val githubUsername = project.properties.getOrSystemEnvOrDefault("GH_USERNAME", "jordanst3wart")
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
            url = uri("https://maven.pkg.github.com/jordanst3wart/gradle-terraform-plugin")
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

