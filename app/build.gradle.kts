plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.affan.screenlapse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.affan.screenlapse"
        minSdk = 24
        targetSdk = 34 // Updated to comply with Play Store requirements
        versionCode = 3
        versionName = "1.1"
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
        viewBinding = true
    }
}

dependencies {
    // MPAndroidChart for stats
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0") // Updated
    implementation("com.google.android.material:material:1.12.0") // Updated
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.material:material:1.11.0") // or latest


    // Lifecycle (if needed for other activities)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.ui.graphics.android)
    implementation(libs.foundation.android)
    implementation(libs.androidx.material3.android)

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1") // Updated
}