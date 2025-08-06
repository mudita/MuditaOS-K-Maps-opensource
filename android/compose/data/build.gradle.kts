plugins {
    id("com.android.library")
    id("com.google.dagger.hilt.android")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.mudita.maps.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 31
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "${projectDir.absolutePath}/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(project(":MapJava"))
    api(libs.coreKtx)
    api(libs.hiltAndroid)
    implementation (libs.roomKtx)
    implementation(libs.retrofit)
    implementation(libs.retrofitGsonConverter)
    implementation(libs.okhttp)

    kapt(libs.roomCompiler)
    kapt(libs.hiltCompiler)
}