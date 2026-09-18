import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.kotlin.plugin.serialization)
}

val gitTag: String? by project

group = "com.icure"
version = gitTag ?: "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    jvm()
    js {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization)
                implementation(libs.kotlin.reflection)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.ksp.symbol.processing.api)
            }
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

mavenPublishing {
    configure(com.vanniktech.maven.publish.KotlinMultiplatform(
        javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
        sourcesJar = true,
    ))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    coordinates(group.toString(), "charix", version.toString())
    signAllPublications()

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
