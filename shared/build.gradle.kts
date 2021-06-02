import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.chromaticnoise.multiplatform-swiftpackage") version "2.0.3"
}

kotlin {
    android()

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget =
        if (System.getenv("SDK_NAME")?.startsWith("iphoneos") == true)
            ::iosArm64
        else
            ::iosX64

    val ios = iosTarget("ios") {
        binaries {
            framework {
                baseName = "Shared"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val iosMain by getting
        val iosTest by getting
    }

    val assembleIosFrameworks by tasks.registering(FatFrameworkTask::class) {
        val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
        val frameworks = kotlin.targets
            .filterIsInstance<KotlinNativeTarget>()
            .filter { it.name.contains("ios", ignoreCase = true) }
            .map { it.binaries.getFramework(mode) }
        baseName = frameworks.random().baseName
        destinationDir = File(buildDir, "ios-frameworks")

        group = "build"
        dependsOn(frameworks.map(Framework::linkTask))
        inputs.property("mode", mode)
        from(frameworks)
    }

    val generateIosArtefacts by tasks.registering(Zip::class) {
        dependsOn(assembleIosFrameworks)
        from(assembleIosFrameworks)
        destinationDirectory.set(layout.buildDirectory.dir("ios-artefact"))
        archiveBaseName.set(assembleIosFrameworks.map { it.fatFrameworkDir.name })
        doLast {
            val jsonFileName = assembleIosFrameworks.map { "${it.baseName}.json" }
            val jsonFile = destinationDirectory.file(jsonFileName).get().asFile
            jsonFile.writeText(
                """
                    {
                        "$VERSION_NAME": "file://${archiveFile.get().asFile.absolutePath}"
                    }
                """.trimIndent()
            )
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}

val VERSION_NAME: String by project
val swiftPackageDirectory = File(buildDir, "swift-package")
val remoteReleaseDir = "https://github.com/kingsleyadio/kmm-playground/releases/download/$VERSION_NAME"

multiplatformSwiftPackage {
    swiftToolsVersion("5.3")
    targetPlatforms {
        iOS { v("13") }
    }
    outputDirectory(swiftPackageDirectory)
//    version =  VERSION_NAME
    zipFileName("Shared.xcframework")
    distributionMode { remote(remoteReleaseDir) }
}


//tasks.getByName("build").dependsOn(packForXcode)
// tasks.getByName("build").dependsOn("createSwiftPackage")
