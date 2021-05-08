// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven {
            url = uri("http://121.36.110.165:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "NexusUser"
                password = "1qaz2wsx"
            }
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.AGP}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlinVersion}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        maven {
            url = uri("http://121.36.110.165:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "NexusUser"
                password = "1qaz2wsx"
            }
        }
    }
}

tasks {
    register<Delete>("clean") {
        delete(rootProject.buildDir)
    }
}
