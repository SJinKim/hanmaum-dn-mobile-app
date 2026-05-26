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
        create("ios") {
            buildConfigField(STRING, "BACKEND_URL", "http://localhost:8080")
            buildConfigField(STRING, "KEYCLOAK_URL", "http://localhost:8091")
        }
        create("st") {
            buildConfigField(STRING, "BACKEND_URL", "https://api.graceops.de")
            buildConfigField(STRING, "KEYCLOAK_URL", "https://auth.graceops.de")
            buildConfigField(STRING, "KEYCLOAK_REALM", "hanmaum-dn-st")
        }
        create("prod") {
            buildConfigField(STRING, "BACKEND_URL", "https://api.graceops.de")
            buildConfigField(STRING, "KEYCLOAK_URL", "https://auth.graceops.de")
            buildConfigField(STRING, "KEYCLOAK_REALM", "hanmaum-dn-prod")
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
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.play.services.location)

            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:34.13.0"))
            implementation(libs.firebase.analytics)
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

    flavorDimensions += "env"
    productFlavors {
        create("dev")  { dimension = "env" }
        create("st")   { dimension = "env" }
        create("prod") { dimension = "env" }
    }

    defaultConfig {
        applicationId = "com.hanmaum.dn.mobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
