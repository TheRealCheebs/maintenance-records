plugins {
    id("com.android.application") version "8.12.2"
    id("org.jetbrains.kotlin.android") version "2.0.0"
    id("org.jetbrains.kotlin.kapt") version "2.0.0"
}

android {
    namespace = "com.github.therealcheebs.maintenancerecords"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.therealcheebs.maintenancerecords"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Nostr libraries
    // Cryptography
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    // JSON
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")

    // Room Database - Updated to version compatible with Kotlin 2.0.0
    implementation("androidx.room:room-runtime:2.7.0-alpha07")
    kapt("androidx.room:room-compiler:2.7.0-alpha07")
    implementation("androidx.room:room-ktx:2.7.0-alpha07")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // kotlinx-metadata-jvm - already at correct version
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
}

// Force the correct version of kotlinx-metadata-jvm
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}
