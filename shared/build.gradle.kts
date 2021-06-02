import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask

plugins {
    kotlin("multiplatform")
    id("com.android.library")
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
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}

val configurationMode = System.getenv("CONFIGURATION") ?: "DEBUG"

val assembleIosFrameworks by tasks.registering(FatFrameworkTask::class) {
    val frameworks = kotlin.targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.name.contains("ios", ignoreCase = true) }
        .map { it.binaries.getFramework(configurationMode) }
    baseName = frameworks.random().baseName
    destinationDir = File(buildDir, "ios-frameworks")

    group = "build"
    dependsOn(frameworks.map(Framework::linkTaskProvider))
    inputs.property("mode", configurationMode)
    from(frameworks)
}

val generateIosArtefacts by tasks.registering(Zip::class) {
    group = "build"
    dependsOn(assembleIosFrameworks)
    from(assembleIosFrameworks)
    destinationDirectory.set(layout.buildDirectory.dir("ios-artefacts"))
    archiveBaseName.set(assembleIosFrameworks.map { it.fatFrameworkDir.name })
    doLast {
        val VERSION_NAME: String by project
        val remoteDownloadDir = "https://github.com/kingsleyadio/kmm-playground/releases/download"
        val artefactLocation = when (configurationMode) {
            "DEBUG" -> "file://${archiveFile.get().asFile.absolutePath}"
            else -> "$remoteDownloadDir/$VERSION_NAME/${archiveFileName.get()}"
        }

        val jsonFileName = assembleIosFrameworks.map { "${it.baseName}.json" }
        val jsonFile = destinationDirectory.file(jsonFileName).get().asFile
        jsonFile.writeText(
            """
                {
                    "$VERSION_NAME": "$artefactLocation"
                }
            """.trimIndent()
        )
    }
}

tasks.named("build").dependsOn(generateIosArtefacts)
