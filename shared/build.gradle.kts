import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

val configurationMode = when (System.getenv("CONFIGURATION")) {
    "RELEASE" -> NativeBuildType.RELEASE
    else -> NativeBuildType.DEBUG
}

kotlin {
    android()

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> Unit = when (configurationMode) {
        NativeBuildType.RELEASE -> ::ios
        else -> { name, configure -> iosX64(name, configure) }
    }

    iosTarget("ios") {
        binaries {
            framework {
                baseName = "Shared"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-native-mt")
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

val assembleIosFrameworks by tasks.registering(FatFrameworkTask::class) {
    val frameworks = kotlin.targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.name.startsWith("ios") }
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
            NativeBuildType.RELEASE -> "$remoteDownloadDir/$VERSION_NAME/${archiveFileName.get()}"
            else -> "file://${archiveFile.get().asFile.absolutePath}"
        }

        val jsonFileName = assembleIosFrameworks.map { "${it.baseName}.json" }
        val jsonFile = destinationDirectory.file(jsonFileName).get().asFile
        jsonFile.writeText("{\n    \"$VERSION_NAME\": \"$artefactLocation\"\n}")

        // Alt: update a $framework.json file at the root of the project for Carthage support
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val jsonF = rootProject.file(jsonFileName)
        val json = when {
            jsonF.exists() -> gson.fromJson(jsonF.readText(), com.google.gson.JsonObject::class.java).asJsonObject
            else -> com.google.gson.JsonObject()
        }
        json.addProperty(VERSION_NAME, artefactLocation)
        jsonF.writeText(gson.toJson(json))
    }
}

tasks.named("build").dependsOn(generateIosArtefacts)
