plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.github.aar0u.quickhub.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.aar0u.quickhub.android"
        minSdk = 21
        //noinspection OldTargetApi
        targetSdk = 33
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("commitSha") as String?)?.let { "1.0.$it" } ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("release.jks")
            storePassword = ""
            keyAlias = "mykey"
            keyPassword = ""
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.github.aar0u:quick-hub:1.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.github.tony19:logback-android:2.0.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
