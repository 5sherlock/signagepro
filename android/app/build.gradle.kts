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
        targetSdk = 29             // V1-only 서명 유지: RK3229(API22) PM이 V2 블록을 못 읽음. Android 11+는 targetSdk<30이면 V2 강제 없음
        versionCode = 10005        // 6자리 빌드번호 = major*10000 + minor*100 + patch (1.0.5 → 표기 010005)
        versionName = "1.0.5"      // 1.0.5: 미디어 캐시 정리를 여유공간 기반으로 + 다운로드 전 옛 캐시 evict — 작은 파티션 보드 ENOSPC로 "배포해도 이전영상/멈춤" 근본 수정. 다음 WebView 개편 = 2.x.x

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
            enableV2Signing = false  // RK3229 ROM이 V2 블록 파싱 불가 → targetSdk=29로 Android 11+ V2 강제 우회
            enableV3Signing = false
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        disable += "ExpiredTargetSdkVersion"  // Play Store 미배포 사이드로드 전용 — targetSdk=29 허용
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

    // 모든 variant 산출물 파일명에 버전 포함 — OTA 업로드 시 서버가 '파일명'에서 버전을 파싱하기 때문.
    // (기존엔 release만 적용돼 debug=app-debug.apk 는 버전이 없어 OTA pendingVersion=null → 푸시 차단되던 문제 근본 해결)
    //   release → signagepro-0.4.31.apk,  debug → signagepro-0.4.31-debug.apk
    applicationVariants.all {
        val variant = this
        val suffix = if (variant.buildType.name == "release") "" else "-${variant.buildType.name}"
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                .outputFileName = "signagepro-${variant.versionName}${suffix}.apk"
        }
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
