plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.appdex"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.appdex"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
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
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core modules ──
    implementation(project(":core:core-arch"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-plugin"))
    implementation(project(":core:core-model"))
    implementation(project(":core:core-common"))

    // ── Feature modules ──
    implementation(project(":feature:feature-files"))
    implementation(project(":feature:feature-editor"))
    implementation(project(":feature:feature-analyzer"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-player"))
    implementation(project(":feature:feature-terminal"))
    implementation(project(":feature:feature-tools"))
    implementation(project(":feature:feature-remote"))

    // ── AndroidX ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)

    // ── Compose ──
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    // ── Hilt ──
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)

    // ── Serialization ──
    implementation(libs.serialization.json)

    // ── Coroutines ──
    implementation(libs.coroutines.android)

    // ── APK parsing ──
    implementation(project(":library:lib-apk"))
}
