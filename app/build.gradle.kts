plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.adulescentia.LUMOS_lib"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
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

}

dependencies {
    //basic
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //additional

    implementation(libs.tasks.vision.v01035)
    implementation(libs.joml.v1108)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.camera.core.v142)
    implementation(libs.androidx.camera.camera2.v142)
    implementation(libs.androidx.camera.lifecycle.v142)
    implementation(libs.androidx.camera.view.v142)
    implementation(libs.guava.v3321android)

}