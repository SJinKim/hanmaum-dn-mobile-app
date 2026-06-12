import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

// Read .env secrets
val envFile = rootProject.file(".env")
val envProps = mutableMapOf<String, String>()
if (envFile.exists()) {
    envFile.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }
        .forEach { line ->
            val idx = line.indexOf('=')
            envProps[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
        }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
    id("com.google.firebase.crashlytics")
}

buildkonfig {
    packageName = "com.hanmaum.dn.mobile"

    defaultConfigs {
        buildConfigField(STRING, "BACKEND_URL", "http://10.0.2.2:8080")
        buildConfigField(STRING, "KEYCLOAK_URL", "http://10.0.2.2:8091")
        buildConfigField(STRING, "KEYCLOAK_REALM", "hanmaum")
        buildConfigField(STRING, "GOOGLE_CALENDAR_ID",    envProps["GOOGLE_CALENDAR_ID"]    ?: "")
        buildConfigField(STRING, "GOOGLE_CALENDAR_API_KEY", envProps["GOOGLE_CALENDAR_API_KEY"] ?: "")
        buildConfigField(STRING, "PCLOUD_FOLDER_ENDPOINT",   envProps["PCLOUD_FOLDER_ENDPOINT"]   ?: "")
        buildConfigField(STRING, "PCLOUD_DOWNLOAD_ENDPOINT", envProps["PCLOUD_DOWNLOAD_ENDPOINT"] ?: "")
    }

    targetConfigs {
        // buildkonfig matches targetConfigs by the exact Kotlin target NAME — there is no "ios"
        // group, so both iOS targets must be configured explicitly (an earlier create("ios")
        // never matched and silently left iOS on the 10.0.2.2 defaults).
        // Local dev falls back to localhost; CI (TestFlight) injects staging URLs via .env so the
        // shipped iOS build reaches a real backend (parity with Android's ST flavor).
        // Android per-flavor URLs are injected via Android buildConfigField below — buildkonfig
        // targetConfigs does not support Android product flavors in KMP projects.
        listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
            create(target) {
                buildConfigField(STRING, "BACKEND_URL", envProps["IOS_BACKEND_URL"] ?: "http://localhost:8080")
                buildConfigField(STRING, "KEYCLOAK_URL", envProps["IOS_KEYCLOAK_URL"] ?: "http://localhost:8091")
                buildConfigField(STRING, "KEYCLOAK_REALM", envProps["IOS_KEYCLOAK_REALM"] ?: "hanmaum")
            }
        }
    }
}

compose.resources {
    packageOfResClass = "hanmaumdnapp.composeapp.generated.resources"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )
    iosTargets.forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // ── iOS simulator link workaround (Kotlin/Native 2.3.0 + Xcode 16) ───────────
    // Kotlin/Native's bundled CoreLocation auto-links the private framework
    // `_LocationEssentials`. That framework exists only in the device runtime, not
    // in the iOS *simulator* SDK, so the simulator link (linkDebugTestIosSimulatorArm64)
    // fails with "framework '_LocationEssentials' not found" — independent of the
    // selected Xcode version.
    //
    // `_LocationEssentials` is absent from the simulator at both link AND runtime, so
    // a plain stub linked at its real install path makes dyld abort at launch
    // ("Library not loaded"). Workaround: generate a .tbd stub under build/ (git-ignored)
    // whose install-name points at libSystem (always present in the simulator). This
    // satisfies the linker's `-framework` lookup and resolves the runtime load command
    // to an existing dylib. No `_LocationEssentials` symbols are referenced, so aliasing
    // it is harmless. Applied to the *simulator* target only — NEVER the iosArm64 device
    // build, which links the real framework.
    //
    // Upstream: https://youtrack.jetbrains.com/issue/KT-71566
    // Remove this block once the bundled CoreLocation no longer references
    // `_LocationEssentials` (i.e. after upgrading to a Kotlin version with the fix).
    if (System.getProperty("os.name").startsWith("Mac")) {
        val stubFramework = project.layout.buildDirectory
            .dir("ci-frameworks/_LocationEssentials.framework").get().asFile
        val stubTbd = stubFramework.resolve("_LocationEssentials.tbd")
        stubFramework.mkdirs()
        stubTbd.writeText(
            """
            --- !tapi-tbd
            tbd-version: 4
            targets: [ arm64-ios-simulator, x86_64-ios-simulator ]
            install-name: '/usr/lib/libSystem.B.dylib'
            ...
            """.trimIndent() + "\n"
        )
        iosTargets.first { it.name == "iosSimulatorArm64" }.binaries.all {
            linkerOpts("-F", stubFramework.parentFile.absolutePath)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.biometric)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.play.services.location)

            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:34.13.0"))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)

            // --- NEU: Networking & JSON ---
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)

            // Pager + announcement
            implementation(libs.foundation)
            implementation(libs.material.icons.extended) // Für Glocke, User, etc.

            implementation(libs.bundles.koin.common)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.datetime)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

        }
        iosMain.dependencies {
            // --- NEU: iOS Engine ---
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

// buildkonfig generates androidMain/BuildKonfig.kt from defaultConfigs, which doesn't support
// Android product flavors in KMP. Rewrite the generated actual in-place after generation so it
// delegates to AGP's per-flavor BuildConfig instead of hardcoded values.
// whenTaskAdded fires as soon as buildkonfig registers its task, regardless of afterEvaluate order.
tasks.whenTaskAdded {
    if (name == "generateBuildKonfig") {
        // Resolve the path and content at configuration time so the doLast action below captures
        // only a serializable Provider<RegularFile> and a String — never the Project. Referencing
        // layout/project inside doLast would pull DefaultProject into the task's serialized state
        // and break the configuration cache.
        val generatedFile = layout.buildDirectory
            .file("buildkonfig/androidMain/com/hanmaum/dn/mobile/BuildKonfig.kt")
        val delegatingSource = """
package com.hanmaum.dn.mobile

import kotlin.String

internal actual object BuildKonfig {
  public actual val BACKEND_URL: String get() = com.hanmaum.dn.mobile.BuildConfig.BACKEND_URL
  public actual val KEYCLOAK_URL: String get() = com.hanmaum.dn.mobile.BuildConfig.KEYCLOAK_URL
  public actual val KEYCLOAK_REALM: String get() = com.hanmaum.dn.mobile.BuildConfig.KEYCLOAK_REALM
  public actual val GOOGLE_CALENDAR_ID: String get() = com.hanmaum.dn.mobile.BuildConfig.GOOGLE_CALENDAR_ID
  public actual val GOOGLE_CALENDAR_API_KEY: String get() = com.hanmaum.dn.mobile.BuildConfig.GOOGLE_CALENDAR_API_KEY
  public actual val PCLOUD_FOLDER_ENDPOINT: String get() = com.hanmaum.dn.mobile.BuildConfig.PCLOUD_FOLDER_ENDPOINT
  public actual val PCLOUD_DOWNLOAD_ENDPOINT: String get() = com.hanmaum.dn.mobile.BuildConfig.PCLOUD_DOWNLOAD_ENDPOINT
}
        """.trimIndent()
        doLast {
            val file = generatedFile.get().asFile
            if (file.exists()) {
                file.writeText(delegatingSource)
            }
        }
    }
}

android {
    namespace = "com.hanmaum.dn.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("String", "KEYCLOAK_URL", "\"http://10.0.2.2:8091\"")
            buildConfigField("String", "KEYCLOAK_REALM", "\"hanmaum\"")
        }
        create("st") {
            dimension = "env"
            buildConfigField("String", "BACKEND_URL", "\"https://api.staging.graceops.de\"")
            buildConfigField("String", "KEYCLOAK_URL", "\"https://auth.graceops.de\"")
            buildConfigField("String", "KEYCLOAK_REALM", "\"hanmaum-dn-st\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BACKEND_URL", "\"https://api.graceops.de\"")
            buildConfigField("String", "KEYCLOAK_URL", "\"https://auth.graceops.de\"")
            buildConfigField("String", "KEYCLOAK_REALM", "\"hanmaum-dn-prod\"")
        }
    }

    defaultConfig {
        applicationId = "com.hanmaum.dn.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GOOGLE_CALENDAR_ID",      "\"${envProps["GOOGLE_CALENDAR_ID"] ?: ""}\"")
        buildConfigField("String", "GOOGLE_CALENDAR_API_KEY", "\"${envProps["GOOGLE_CALENDAR_API_KEY"] ?: ""}\"")
        buildConfigField("String", "PCLOUD_FOLDER_ENDPOINT",   "\"${envProps["PCLOUD_FOLDER_ENDPOINT"] ?: ""}\"")
        buildConfigField("String", "PCLOUD_DOWNLOAD_ENDPOINT", "\"${envProps["PCLOUD_DOWNLOAD_ENDPOINT"] ?: ""}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "internal-testers-graceops"
                releaseNotes = "Debug build – ${System.getenv("RELEASE_NOTES") ?: "manual distribution"}"
            }
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            firebaseAppDistribution {
                artifactType = "APK"
                groups = "internal-testers-graceops"
                releaseNotes = "Release build – ${System.getenv("RELEASE_NOTES") ?: "manual distribution"}"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.ui.tooling)
}
