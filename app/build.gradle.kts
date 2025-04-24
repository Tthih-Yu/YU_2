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
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        // 调试版本通常不启用混淆，以便更容易调试
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    
    // Material Icons扩展包 (版本由BOM管理)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    
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
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Kotlin 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Gson for JSON processing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Or use Moshi
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // Check for latest version
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // For debugging
    implementation("org.jsoup:jsoup:1.15.3") // For parsing HTML (CAS login form)

    // Hilt for Dependency Injection (Optional but recommended)
    // implementation("com.google.dagger:hilt-android:2.48") // Check latest version
    // kapt("com.google.dagger:hilt-compiler:2.48")

    // Accompanist SwipeRefresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.27.0") // Check latest version

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}