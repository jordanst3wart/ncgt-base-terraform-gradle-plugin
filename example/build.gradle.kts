

plugins {
    id("foo.bar.terraform") version("CLEAN-UP-SNAPSHOT")
    // id("com.newscorp.gt.gradle.terraform")
}

// /Users/stewartj/.m2/repository/com/newscorp/gt/gradle/terraform
// com.newscorp.gt.gradle.terraform.gradle.plugin
terraform {
    executable(mapOf("version" to "1.11.1"))
}

/*terraformSourceSets {
    main {
        srcDir = file('main/tf')
        backendText("hi") // backend file...
        variables {
            file('vars.tfvars')
        }
    }
}*/
//