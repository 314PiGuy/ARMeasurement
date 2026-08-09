plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.armeasurement"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.armeasurement"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.constraintlayout)
// Community-maintained Sceneform / ARCore wrapper
    implementation("io.github.sceneview:arsceneview:2.2.1")
}