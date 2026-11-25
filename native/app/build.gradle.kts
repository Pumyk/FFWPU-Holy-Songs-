plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigning = listOf(
    "HPHC_RELEASE_KEYSTORE", "HPHC_RELEASE_STORE_PASSWORD",
    "HPHC_RELEASE_KEY_ALIAS", "HPHC_RELEASE_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
val missingReleaseSigning = releaseSigning.filterValues { it == null }.keys

android {
    namespace = "com.hphc.library"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hphc.library.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "3.5.3"
    }

    signingConfigs {
        create("release") {
            // Keep AGP signing validation enabled even if credentials are missing.
            // The placeholder values are not secrets and cannot sign anything.
            storeFile = releaseSigning["HPHC_RELEASE_KEYSTORE"]?.let { rootProject.file(it) }
                ?: rootProject.file(".release-keystore-not-configured")
            storePassword = releaseSigning["HPHC_RELEASE_STORE_PASSWORD"] ?: "not-configured"
            keyAlias = releaseSigning["HPHC_RELEASE_KEY_ALIAS"] ?: "not-configured"
            keyPassword = releaseSigning["HPHC_RELEASE_KEY_PASSWORD"] ?: "not-configured"
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
            applicationIdSuffix = ".debug"
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

tasks.configureEach {
    if (name == "validateSigningRelease") {
        doFirst {
            check(missingReleaseSigning.isEmpty()) {
                "Release signing is not configured. Set: ${missingReleaseSigning.joinToString()}"
            }
        }
    }
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
