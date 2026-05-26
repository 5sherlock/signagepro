plugins {
    id("com.android.application")
}

// 진단용 최소 APK — 의존성 0, DEX 1개, 순수 Java.
// 메인 앱과 동일한 build-tools/AGP/compileSdk/minSdk 사용.
// 이 APK가 RK3229 기기에 설치되면 → 원인은 메인 앱의 의존성/DEX 볼륨.
// 설치 안 되면 → build-tools 35 / AGP 8.5 산출물 자체가 비호환.
android {
    namespace = "com.signagepro.probe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.signagepro.probe"
        minSdk = 22
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = false
            enableV3Signing = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
