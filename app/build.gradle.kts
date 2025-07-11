plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" // room
    // firebase
    id("com.google.gms.google-services") // google services gradle plugin
}

android {
    namespace = "com.example.canstone2"
    compileSdk = 35

    buildFeatures{
        viewBinding = true
        dataBinding = true
    }

    defaultConfig {
        applicationId = "com.example.canstone2"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit (REST API)
//    implementation(libs.retrofit)
//    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.retrofit2.kotlinx.serialization.converter)
    // OkHttp
//    implementation("com.squareup.okhttp3:okhttp")
//    implementation(libs.logging.interceptor)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.androidx.room.compiler.v271)
    // firebase
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    implementation("com.google.firebase:firebase-firestore")
    // firebase coroutine
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // ONNX (AI)
    implementation ("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
    // ViewModel KTX (viewModelScope 포함)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    // LiveData KTX
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    // Activity KTX
    implementation("androidx.activity:activity-ktx:1.10.0")
    // Fragment KTX
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // front 병합
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // implementation(libs.androidx.ui.text.android)
}
