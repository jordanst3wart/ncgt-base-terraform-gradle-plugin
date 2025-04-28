/*import org.ysb33r.gradle.terraform.TerraformRCExtension


plugins {
    id("foo.bar.terraform") version ("CLEAN-UP-4-SNAPSHOT")
    // id("com.newscorp.gt.gradle.terraform")
}

// /Users/stewartj/.m2/repository/com/newscorp/gt/gradle/terraform
// com.newscorp.gt.gradle.terraform.gradle.plugin
terraform {
    useAwsEnvironment()
    useGoogleEnvironment()
    setLockTimeout(30)
    setParallel(11)
    executable(mapOf("version" to "1.10.1"))
}

// terraformRCExtension define extensions...
project.rootProject.extensions.getByType<TerraformRCExtension>().apply {
    pluginCacheMayBreakDependencyLockFile = true
}

terraformSourceSets {
    create("main") {
        setSrcDir("src/main/tf")
        setBackendText("# foo = bar") // TODO needs to be defined..., could be optional
    }
}*/
