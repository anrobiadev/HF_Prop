plugins {
    alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Required for Kotlin 2.0+!
    id("com.chaquo.python")
}

android {
    namespace = "com.example.hfpropagation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.hfpropagation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Required by Chaquopy to build the Python engine
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    // We removed the old composeOptions block here because Kotlin 2.1.0 doesn't need it!
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Chaquopy Python Configuration
chaquopy {
    defaultConfig {
        version = "3.11"
    }
}


dependencies {
    implementation("com.google.android.material:material:1.11.0")
    // Core Android dependencies (these are usually safe in the dictionary)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Explicit Jetpack Compose dependencies (Bypassing the 'libs' dictionary)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- Custom App Dependencies ---

    // OSMDroid for the offline map
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // WorkManager for background space weather fetching
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    // In build.gradle.kts
    implementation("com.airbnb.android:lottie-compose:6.1.0")
}