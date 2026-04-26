import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING




plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
}

buildkonfig {
    packageName = "com.hanmaum.dn.mobile"

    defaultConfigs {
        buildConfigField(STRING, "BACKEND_URL", "http://10.0.2.2:8080")
        buildConfigField(STRING, "KEYCLOAK_URL", "http://10.0.2.2:8091")
    }

    targetConfigs {
        create("android") {
            // nutzt defaults
        }
        create("ios") {
            buildConfigField(STRING, "BACKEND_URL", "http://localhost:8080")
            buildConfigField(STRING, "KEYCLOAK_URL", "http://localhost:8091")
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
            // --- NEU: Android Engine ---
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.play.services.location)
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

        }
        iosMain.dependencies {
            // --- NEU: iOS Engine ---
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.hanmaum.dn.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

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
        getByName("release") {
            isMinifyEnabled = false
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
