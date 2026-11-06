import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mangaauthority.tvai"
    // Material 3 Expressive (material3 1.5.0-alpha) and current Compose need compileSdk 36.
    // minSdk 24 / targetSdk 34 keep behaviour identical on older TV boxes.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mangaauthority.tvai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/DEPENDENCIES")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Networking: OkHttp for the Gemini REST API (JSON uses the built-in org.json)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Expressive APIs live in the 1.5.0 alphas, so this overrides the BOM's 1.4.0.
    implementation("androidx.compose.material3:material3:1.5.0-alpha26")

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
}
