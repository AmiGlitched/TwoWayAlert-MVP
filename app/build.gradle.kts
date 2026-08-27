plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.myapplication.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.myapplication.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    androidResources{
        noCompress += listOf("fst", "mdl", "txt", "conf", "raw", "slurp")
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
    buildFeatures {
        compose = true
        mlModelBinding = true
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {

    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
        implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Firebase BoM (Bill of Materials) ensures all Firebase versions are compatible
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

// Firebase Authentication and Cloud Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // Vosk Speech Recognition Toolkit
    implementation ("com.alphacephei:vosk-android:0.3.47")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(libs.jna){
        artifact{
            type = "aar"
        }
    }

}