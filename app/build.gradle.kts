plugins {
    id("com.android.application")
}

android {
    namespace = "ai.humnod.genai.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "ai.humnod.genai.app"
        minSdk = 27
        targetSdk = 34
        versionCode = 5
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ONNX Runtime with GenAI
    implementation("com.microsoft.onnxruntime:onnxruntime-android:latest.release")
    implementation(files("libs/onnxruntime-genai-android-0.4.0-dev.aar"))

    //MarkDown
    implementation ("io.noties.markwon:core:4.6.2")

    implementation("com.google.mlkit:text-recognition:16.0.1")

    implementation ("com.itextpdf:itext7-core:7.2.3")


}