plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Optional signing properties, injected by the CI workflow (or set locally).
val keystorePath: String? = (project.findProperty("MYT_KEYSTORE_PATH") as String?)?.takeIf { it.isNotBlank() }
val keystorePass: String? = (project.findProperty("MYT_KEYSTORE_PASS") as String?)?.takeIf { it.isNotBlank() }
val keystoreAlias: String? = (project.findProperty("MYT_KEY_ALIAS") as String?)?.takeIf { it.isNotBlank() }
val keystoreKeyPass: String? = (project.findProperty("MYT_KEY_PASS") as String?)?.takeIf { it.isNotBlank() }

// Sign the release build only when the full keystore info is present;
// otherwise fall back to debug signing so builds never break locally.
val hasReleaseSigning = keystorePath != null && keystorePass != null && keystoreAlias != null && keystoreKeyPass != null
println("Myt: release signing = $hasReleaseSigning (path=${keystorePath != null}, pass=${keystorePass != null}, alias=${keystoreAlias != null}, keyPass=${keystoreKeyPass != null})")

android {
    namespace = "com.myt.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myt.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Free key from https://devs.jamendo.com (set MYT_JAMENDO_CLIENT_ID in gradle.properties
        // or as a GitHub Actions secret). Leave empty to run with local music only.
        val jamendoClientId: String = (project.findProperty("MYT_JAMENDO_CLIENT_ID") as String?)
            ?.takeIf { it.isNotBlank() } ?: ""
        buildConfigField("String", "JAMENDO_CLIENT_ID", "\"$jamendoClientId\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(keystorePath!!)
                storePassword = keystorePass
                keyAlias = keystoreAlias
                keyPassword = keystoreKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}