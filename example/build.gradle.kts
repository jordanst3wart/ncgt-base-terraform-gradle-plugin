import org.ysb33r.gradle.terraform.TerraformRCExtension


plugins {
    // might need to be published to access
    id("foo.bar.terraform") // version ("FIX-VERSION2-SNAPSHOT")
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
project.extensions.getByType<TerraformRCExtension>().apply {
    pluginCacheMayBreakDependencyLockFile = true
}

terraformSourceSets {
    create("main") {
        setSrcDir("src/main/tf")
        setBackendText("# foo = bar") // TODO needs to be defined..., could be optional
    }
    create("aws") {
        setSrcDir("src/aws/tf")
        setBackendText("# foo = bar") // TODO needs to be defined..., could be optional
    }
}
