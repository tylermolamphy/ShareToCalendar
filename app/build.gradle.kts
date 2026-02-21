plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tylermolamphy.sharetocalendar"
    compileSdk = 35

    val versionCodeProp  = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
    val keystorePath     = System.getenv("KEYSTORE_PATH")
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val keyAliasProp     = System.getenv("KEY_ALIAS")
    val keyPasswordProp  = System.getenv("KEY_PASSWORD")

    defaultConfig {
        applicationId = "net.molamphy.tyler.sharetocal"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeProp
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePath != null && keystorePassword != null &&
            keyAliasProp != null && keyPasswordProp != null) {
            create("release") {
                storeFile     = file(keystorePath)
                storePassword = keystorePassword
                keyAlias      = keyAliasProp
                keyPassword   = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            // signingConfig is null when env vars are absent (local/PR builds); that's intentional.
            // CI sets all four env vars and gets a properly signed APK.
            signingConfig = signingConfigs.findByName("release")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Suppress internal lint crash in NonNullableMutableLiveDataDetector (AGP tooling bug)
        disable += "NullSafeMutableLiveData"
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.compose.material:material-icons-core")
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
