plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android) // Uncomment if needed based on your libs.versions.toml
    alias(libs.plugins.kotlin.compose) // Required for Kotlin 2.0+
    id("com.chaquo.python") // Notice: No version here!
    //id("com.chaquo.python") version "15.0.1" apply false
}

android {
    namespace = "com.example.hfpropagation"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = "com.example.hfpropagation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Required by Chaquopy to build the Python C/Fortran extensions
        // Python 3.12+ only supports 64-bit ABIs (arm64-v8a and x86_64)
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            // Nu activa minificarea aici, altfel debugger-ul o să o ia razna
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// --- CHAQUOPY PYTHON CONFIGURATION ---
chaquopy {
    defaultConfig {
        version = "3.11"
        buildPython("py", "-3.11")
        pip {
            install("numpy")
        }
    }
}

dependencies {
    // --- THIS FIXES THE AAPT THEME ERROR ---
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)


    // Explicit Jetpack Compose dependencies
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Custom App Dependencies ---

    // OSMDroid for the offline footprint map
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // WorkManager for background space weather fetching
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Location services (if you implement auto-location later)
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Lottie for the startup splash screen animations
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}