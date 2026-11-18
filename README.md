# Orbit Chat

Native Java chat client for square-screen watches and compact Android devices. Supports Android 8.0 (API 26) and newer, OpenAI-compatible Chat Completions, and Anthropic Messages. The app uses platform Views, `HttpURLConnection`, SQLite, and private SharedPreferences; it has no Kotlin, Compose, AndroidX, or runtime networking dependencies.

## Build

Project settings: Android Gradle Plugin 8.7.3, Gradle 8.9 wrapper distribution, Android SDK Platform 35, `minSdk 26`, `targetSdk 28`. Use JDK 17 for the supported AGP 8.7 toolchain.

The repository does not include `gradle-wrapper.jar`. With Gradle installed, generate the wrapper using `gradle wrapper --gradle-version 8.9`, then run `gradlew.bat :app:assembleDebug` on Windows or `./gradlew :app:assembleDebug` on Unix-like systems.

The Java sources were independently compiled against Android 35 `android.jar` with JDK 25 and Java 8 source/target compatibility. A Gradle APK build and runtime/device test have not been completed in the current environment.

## Integration and acceptance

See [CONTRACT.md](CONTRACT.md) for the actual package/API contract, provider endpoint forms, worker-thread requirements, and device/API acceptance matrix. Main sources live in `app/src/main/java/com/example/watchchat/`.
