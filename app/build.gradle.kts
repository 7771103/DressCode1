plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dresscode1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dresscode1"
        minSdk = 24
        targetSdk = 35
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
    
    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lottie)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Lifecycle components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    
    // Room components
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    // Glide for image loading
    implementation(libs.glide)
    
    // CircleImageView for circular avatars
    implementation(libs.circleimageview)
    
    // JWT for QWeather API authentication
    implementation(libs.jwt)
    runtimeOnly(libs.jwt.impl)
    runtimeOnly(libs.jwt.jackson)
    // BouncyCastle for Ed25519 support
    implementation(libs.bouncycastle)
    // BouncyCastle PKIX for PEM parsing (openssl support)
    implementation(libs.bouncycastle.pkix)
    // BouncyCastle Util for Ed25519 utilities
    implementation(libs.bouncycastle.util)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}