plugins {
    id("com.android.application")
}

android {
    namespace = "com.ambuj.youtubeauto"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ambuj.youtubeauto"
        minSdk = 35
        targetSdk = 37
        versionCode = 10
        versionName = "0.1.9-cast-viewer"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.car.app:app:1.7.0")
}
