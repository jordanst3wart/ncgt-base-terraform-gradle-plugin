import org.ysb33r.gradle.terraform.TerraformRCExtension

plugins {
    id("foo.bar.terraform")
}

terraform {
    useAwsEnvironment()
    useGoogleEnvironment()
    setLockTimeout(31)
    setParallel(11)
    executable(mapOf("version" to "1.10.1"))
}

// terraformRCExtension define extensions...
project.extensions.getByType<TerraformRCExtension>().apply {
    pluginCacheMayBreakDependencyLockFile = true
}

terraformSourceSets {
    create("main") {
        srcDir.set(File("src/main/tf"))
        backendText.set("# foo = bar") // TODO needs to be defined..., could be optional
    }
    create("aws") {
        srcDir.set(File("src/aws/tf"))
        backendText.set("# foo = bar") // TODO needs to be defined..., could be optional
    }
}
