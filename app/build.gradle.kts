import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// API keys are read from local.properties (git-ignored), never from source.
//   ebird.key=YOUR_EBIRD_KEY
//   xenocanto.key=YOUR_XENO_CANTO_KEY
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localKey(name: String): String =
    (localProps.getProperty(name) ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.nightskywings"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nightskywings"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "EBIRD_KEY", "\"${localKey("ebird.key")}\"")
        buildConfigField("String", "XENO_KEY", "\"${localKey("xenocanto.key")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Provides Task.await() used for the location lookup.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil:2.7.0")
    // org.json is part of Android itself, so it is not an app dependency.
    // Local JVM unit tests need a real copy because the SDK's android.jar only has stubs.

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
