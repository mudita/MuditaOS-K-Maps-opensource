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
    namespace = "com.mudita.map.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "IS_PREMIERE_VERSION", "true")
        buildConfigField("boolean", "IS_PLAN_ROUTE_ENABLED", "true")
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

    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    api(project(mapOf("path" to ":MapJava")))
    api(project(mapOf("path" to ":MapApi")))
    api(project(":frontitude"))

    api(libs.kompaktUI)
    api(libs.material3)
    api(libs.composeFoundation)
    api(libs.composeNavigation)
    api(libs.gson)

    api(libs.composeUIToolingPreview)
    debugApi(libs.composeUITooling)
    api(libs.timber)

    kapt(libs.hiltCompiler)

    implementation(libs.datastore)
    implementation(libs.hiltAndroid)
    api(libs.hiltNavigation)
}