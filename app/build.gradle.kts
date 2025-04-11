plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.tthih.yu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tthih.yu"
        minSdk = 31
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Material Icons扩展包
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    
    // Compose与LiveData的集成
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4")
    
    // Lifecycle组件
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // CardView 支持
    implementation("androidx.cardview:cardview:1.0.0")
    
    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Material Design 组件
    implementation("com.google.android.material:material:1.10.0")
    
    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")
    
    // Kotlin 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}