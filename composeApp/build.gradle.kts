import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Navigation & Lifecycle
            implementation(libs.cmp.navigation.compose)
            implementation(libs.cmp.lifecycle.viewmodel)
            implementation(libs.cmp.lifecycle.runtime)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // GitLive Firebase
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)
            implementation(libs.gitlive.firebase.storage)

            // Coil 3
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Serialization & Coroutines & Datetime
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.play.services)

            // Firebase Android SDK for GitLive runtime
            implementation(libs.firebase.auth.android)
            implementation(libs.firebase.firestore.android)
            implementation(libs.firebase.storage.android)
            implementation(libs.firebase.common.android)

            // Koin Android
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // Android Location & Auth
            implementation(libs.play.services.location)
            implementation(libs.play.services.auth)

            // MapLibre Android
            implementation(libs.maplibre.sdk)

            // Ktor OkHttp engine
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            // Ktor Darwin engine
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

compose.resources {
    packageOfResClass = "com.loregate.app.generated.resources"
}

android {
    namespace = "com.loregate.app"
    compileSdk = 37

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")

    defaultConfig {
        applicationId = "com.loregate.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        val mapTilerApiKey = localProperties.getProperty("MAPTILER_API_KEY", "")
        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")

        val cloudinaryCloudName = localProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")

        val cloudinaryUploadPreset = localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET", "")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
