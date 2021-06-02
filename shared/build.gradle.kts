import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

    val packForXcode by tasks.creating(Sync::class) {
        val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
        val framework = ios.binaries.getFramework(mode)
        val targetDir = File(buildDir, "xcode-frameworks")

        group = "build"
        dependsOn(framework.linkTask)
        inputs.property("mode", mode)

        from({ framework.outputDirectory })
        into(targetDir)
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

val generateIosArtefacts by  tasks.creating(Sync::class) {
    val outputDir = File(buildDir, "ios-framework")
    dependsOn("createSwiftPackage")
    from(swiftPackageDirectory)
    into(outputDir)
    doLast {
        val zipFile = outputDir.listFiles { f: File -> f.extension == "zip" }?.firstOrNull()
        if (zipFile != null) {
            File(outputDir, "Shared.json")
                .writer()
                .use {
                    it.write("""
                    {
                        "$VERSION_NAME": "$remoteReleaseDir/${zipFile.name}"
                    }
                """.trimIndent())
                }
        }
    }
}

//val packForCarthage by tasks.registering(Zip::class) {
//    val createXCFramework by tasks.getting(Exec::class)
//    dependsOn(createXCFramework)
//    from({ "$buildDir/swift-package" })
//    archiveBaseName.set("Shared.xcframework.zip")
//    destinationDirectory.set(File(buildDir, "carthage-frameworks"))
//}

//tasks.getByName("build").dependsOn(packForXcode)
tasks.getByName("build").dependsOn("createSwiftPackage")
