import java.util.Date
import java.text.SimpleDateFormat

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
        targetSdk = 34             // 동일 디바이스에서 targetSdk 36도 설치 확인됨
        versionCode = 4
        versionName = "0.4.0"

        // 빌드 날짜를 BuildConfig에 자동 삽입 (관제 화면 버전 표시용)
        val buildDate = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")

        // RK3229 = ARMv7 32bit. 다른 ABI 빌드 생략으로 APK 크기·복잡도 감소
        ndk {
            abiFilters += listOf("armeabi-v7a")
        }

        // 일부 구형 Android 5.x 커스텀 ROM에서 native multidex가 불안정 →
        // multidex 라이브러리로 우회
        multiDexEnabled = true
    }

    // RK3229 커스텀 ROM(Android 5.1.1)의 구형 zip 파서는 APK Signing Block(v2/v3)을
    // 못 다뤄 "구문분석 오류"를 낸다. v1(JAR) 전용 서명으로 고전 APK처럼 처리되게 함.
    signingConfigs {
        // 프로젝트 전용 키 — 어떤 PC에서 빌드해도 동일한 서명
        create("signagepro") {
            storeFile = file("signagepro.keystore")
            storePassword = "signagepro2026"
            keyAlias = "signagepro"
            keyPassword = "signagepro2026"
            enableV1Signing = true   // Android 5.1.1 구형 ROM 호환
            enableV2Signing = false
            enableV3Signing = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("signagepro")
        }
        release {
            signingConfig = signingConfigs.getByName("signagepro")
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
