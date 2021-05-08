plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("ComBuild"){
            id = "ComBuild"
            implementationClass = "com.cctv.cbox.ComBuild"
        }
    }
}

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

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

object Plugins {
    const val AGP = "4.2.0"
    const val KOTLIN = "1.4.31"
}

dependencies {
    //gradle sdk
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Plugins.KOTLIN}")
    implementation("com.android.tools.build:gradle:${Plugins.AGP}")
}