plugins {
    idea
    id("org.ysb33r.os") version "1.0.0"
    id("org.ajoberstar.git-publish") version "3.0.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
        // mavenLocal()
    }
}