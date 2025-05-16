plugins {
    idea
    id("org.ajoberstar.git-publish") version "3.0.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
        // mavenLocal()
    }
}