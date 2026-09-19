plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.offlinecounter"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "app.offlinecounter"
        minSdk = 29
        targetSdk = 36
        versionCode = 23
        versionName = "1.0.2-dev"
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions.unitTests {
        isIncludeAndroidResources = true
        all {
            it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
