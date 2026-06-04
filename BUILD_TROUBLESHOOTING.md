# APK 빌드 트러블슈팅

## Android Studio JBR 손상으로 인한 Gradle 빌드 실패

### 증상

`build_apk.ps1` 또는 `gradlew assembleRelease` 실행 시 아래 오류 발생:

```
FAILURE: Build failed with an exception.

* What went wrong:
Unable to determine version for JDK located at C:\Program Files\Android\Android Studio\jbr.
Reason: Command returned unexpected result code: 1
Error output:
Error: could not open `C:\Program Files\Android\Android Studio\jbr\lib\jvm.cfg'
```

### 원인

Gradle의 Java Toolchain 자동 감지가 Android Studio 내장 JBR(JetBrains Runtime)을 탐지하는데,
해당 경로(`C:\Program Files\Android\Android Studio\jbr`)의 `jvm.cfg` 파일이 없거나 손상된 상태.

JAVA_HOME을 다른 JDK로 설정해도, Gradle이 시스템 전체 JDK를 스캔하면서 깨진 JBR을 계속 감지함.

### 해결 방법

`gradlew` 실행 시 아래 두 옵션을 함께 지정:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot"

.\gradlew assembleRelease `
  "-Dorg.gradle.java.home=C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot" `
  "-Dorg.gradle.java.installations.auto-detect=false"
```

| 옵션 | 역할 |
|------|------|
| `org.gradle.java.home` | Gradle이 사용할 JDK를 명시적으로 지정 |
| `org.gradle.java.installations.auto-detect=false` | 시스템 JDK 자동 스캔 비활성화 (깨진 JBR 탐지 차단) |

### 확인된 동작 환경

- OS: Windows 11 Enterprise
- 사용 JDK: Microsoft OpenJDK 17.0.17 (`C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot`)
- 빌드 성공 버전: v0.4.17 (versionCode 18)

### 항구적 해결 (선택사항)

`android/gradle.properties`에 아래 두 줄 추가하면 매번 옵션을 입력하지 않아도 됨:

```properties
org.gradle.java.home=C:\\Program Files\\Microsoft\\jdk-17.0.17.10-hotspot
org.gradle.java.installations.auto-detect=false
```

---

## 버전 올림 후 OTA 업데이트가 안 되는 경우

### 증상

APK를 서버에 올렸는데 기기가 자동 업데이트를 받지 않음.

### 원인

서버의 OTA 자동 발동 조건: `currentVersion !== targetVersion` (문자열 비교)
기기 현재 버전과 서버 APK 버전이 같으면 업데이트 스킵.

### 해결 방법

`android/app/build.gradle.kts`에서 버전을 올리고 재빌드:

```kotlin
versionCode = 18       // 1 증가
versionName = "0.4.17" // 버전명 변경
```

이후 `build_apk.ps1` 실행하면 빌드 + 서버 업로드까지 자동 처리됨.
