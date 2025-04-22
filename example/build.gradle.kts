import org.ysb33r.gradle.terraform.TerraformRCExtension


plugins {
    id("foo.bar.terraform") version("CLEAN-UP-SNAPSHOT")
    // id("com.newscorp.gt.gradle.terraform")
}

// /Users/stewartj/.m2/repository/com/newscorp/gt/gradle/terraform
// com.newscorp.gt.gradle.terraform.gradle.plugin
terraform {
    useAwsEnvironment()
    useGoogleEnvironment()
    executable(mapOf("version" to "1.10.1"))
}

// terraformRCExtension define extensions...
/*terraformrc {

}*/

terraformSourceSets {
    create("main") {
        setSrcDir("src/main/tf")
        setBackendText("foo = bar") // TODO needs to be defined...
    }
}

tasks.register("LogInfo") {
    doLast {
        println("Terraform source sets: ${terraformSourceSets.named("main").get().srcDir.get()}")
    }
}
// logger.info()
    /*main {
        srcDir = file('main/tf')
        backendText("hi") // backend file...
        variables {
            file('vars.tfvars')
        }
    }*/
//