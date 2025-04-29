plugins {
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

repositories {
    mavenCentral()
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


dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    api("org.ysb33r.gradle:grolifant-herd:3.0.1")
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
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
