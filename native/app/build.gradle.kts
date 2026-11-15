plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hphc.library"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hphc.library"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "3.5.1"
    }

    /**
     * One permanent signing key, checked in with the project on purpose:
     * every build (this machine, CI, any future machine) is signed with the
     * same certificate, so a new APK installs straight over the old one and
     * Android never reports a signature mismatch.
     *
     * Losing this file means losing the ability to update installed copies -
     * keep it with the repo, not just on one computer.
     */
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("hphc-release.keystore")
            storePassword = "hphc-library-2026"
            keyAlias = "hphc"
            keyPassword = "hphc-library-2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Same key as release: an APK from CI or a local build installs
            // over whatever is already on the phone.
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += "pdf" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
