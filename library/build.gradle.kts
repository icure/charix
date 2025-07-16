import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.gitVersion) apply(true)
}

val gitVersion: String? by project

group = "com.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.symbol.processing.api)
                implementation(libs.kotlin.serialization)
                implementation(libs.kotlin.reflection)
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}

val localPropertiesFile = File(rootDir, "local.properties")
val localProperties = Properties()

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val githubUsername = localProperties["githubUsername"] as String
val githubPassword = localProperties["githubPassword"] as String

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/icure/charix")
            credentials {
                username = githubUsername
                password = githubPassword
            }
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "charix", version.toString())

    pom {
        name = "Charix"
        description = "Serializable representation of KSP models"
        inceptionYear = "2024"
        url = "https://github.com/icure/charix"
        licenses {
            license {
                name.set("MIT License")
                url.set("https://choosealicense.com/licenses/mit/")
                distribution.set("https://choosealicense.com/licenses/mit/")
            }
        }
        developers {
            developer {
                id.set("icure")
                name.set("iCure")
                url.set("https://github.com/iCure/")
            }
        }
        scm {
            url.set("https://github.com/icure/charix")
            connection.set("scm:git:git://github.com/icure/charix.git")
            developerConnection.set("scm:git:ssh://git@github.com:icure/charix.git")
        }
    }
}
