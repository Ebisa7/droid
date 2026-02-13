plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ltquiz.test"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ltquiz.test"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.experimental.ExperimentalTypeInference",
            "-opt-in=kotlin.experimental.ExperimentalBreakContinue"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material:material-icons-core")
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // WebSocket dependencies
    implementation(libs.okhttp)
    
    // Serialization dependencies
    implementation(libs.kotlinx.serialization.json)
    
    // QR Code dependencies
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    
    // MLKit and Camera dependencies
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    
    // WebRTC dependencies
    implementation(libs.webrtc.android)
    
    // Navigation dependencies
    implementation(libs.navigation.compose)
    implementation(libs.navigation.hilt)
    
    // Animation dependencies
    implementation(libs.lottie.compose)
    
    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
}
