plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.signagepro.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.signagepro.player"
        minSdk = 22                // Android 5.1.1 (RK3229 U4X+ CM)
        targetSdk = 30             // 스코프 스토리지 회피 + 5.1.1 호환
        versionCode = 1
        versionName = "0.1.0"

        // RK3229 = ARMv7 32bit. 다른 ABI 빌드 생략으로 APK 크기·복잡도 감소
        ndk {
            abiFilters += listOf("armeabi-v7a")
        }

        // 일부 구형 Android 5.x 커스텀 ROM에서 native multidex가 불안정 →
        // multidex 라이브러리로 우회
        multiDexEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.multidex:multidex:2.0.1")
    // Material Components 제거 — dex 수 감소 (이번 단계 UI에는 불필요)

    // 미디어 재생 (Media3 / ExoPlayer)
    val media3 = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // HTTP/JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")

    // Socket.io
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }

    // Android 5.1.1 호환용 코어 라이브러리 desugaring (java.time 등)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
