
plugins {
    // builds to: ~/.m2/repository/foo/bar/terraform/foo.bar.terraform.gradle.plugin/INSTALLER3-SNAPSHOT
    id("foo.bar.terraform") version "INSTALLER3-SNAPSHOT"
}

terraformSetup {
    //executable.set()
    //executableVersion
    //executableVersion.set("1.10.1")
}

// TODO add TerraformSetup...
terraform {
    useAwsEnvironment()
    useGoogleEnvironment()
    setLockTimeout(31)
    setParallel(11)
    // this should be removed
    //executable(mapOf("version" to "1.10.1"))
}

// terraformRCExtension define extensions...
//project.extensions.getByType<TerraformSetupExtension>().apply {
//    pluginCacheMayBreakDependencyLockFile = true
//}

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
