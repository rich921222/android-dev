plugins {
    alias(libs.plugins.android.application) // 已包含 com.android.application
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Firebase Plugin
}

android {
    namespace = "com.example.dinner_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dinner_app"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ✅ 更新 Firebase 依賴版本
    //implementation("com.google.firebase:firebase-database:20.3.1") // Firebase Realtime Database (最新版)
    //implementation("com.google.firebase:firebase-auth:22.3.1") // Firebase Authentication (最新版)
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database") // 這行保證 Firebase Realtime Database 被加入
    implementation("com.google.firebase:firebase-auth") // Firebase Authentication

    // ✅ 加這行 Flexbox Layout
    implementation("com.google.android.flexbox:flexbox:3.0.0")
}
