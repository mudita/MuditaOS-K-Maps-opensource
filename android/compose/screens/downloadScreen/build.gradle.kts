plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
}

apply(from = rootProject.file("test-common.gradle"))

android {
    namespace = "com.mudita.download"
    compileSdk = 34

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
        debug {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(project(":frontitude"))
    implementation(project(":pagination"))
    implementation(project(":frontitude"))

    implementation(project(":data"))
    implementation(project(":common"))
    implementation(project(":MapApi"))
    implementation(libs.accompanistPager)
    implementation(libs.accompanistPagerIndicators)
    implementation(libs.hiltNavigation)
    implementation(libs.hiltAndroid)
    implementation(libs.composeUIToolingPreview)
    implementation(libs.retrofit)
    implementation(libs.retrofitGsonConverter)
    implementation(libs.okhttp)

    kapt(libs.hiltCompiler)

    debugImplementation(libs.composeUITooling)
}