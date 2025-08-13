plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.57" // Hilt plugin
    kotlin("kapt") // For annotation processing
    id("com.google.gms.google-services")
}

android {
    namespace = "com.migge.supershoppercartapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.migge.supershoppercartapp"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.runtime)

    // Google Services
    implementation(libs.play.services.auth)
    implementation("androidx.credentials:credentials:1.6.0-alpha04")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-alpha04")
    // Hilt Dependencies
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.googleid)
    kapt(libs.hilt.android.compiler)

    // Firebase Dependencies managed by BOM
    implementation(platform(libs.firebase.bom))

    // https://mvnrepository.com/artifact/com.google.firebase/firebase-auth-ktx
    // Note: firebase-crashlytics-buildtools is a plugin dependency, not an implementation dependency.
    // It should be declared in the plugins block if needed.
    // Retrofit
    implementation(libs.converter.gson)


    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Networking
    implementation(libs.retrofit)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}