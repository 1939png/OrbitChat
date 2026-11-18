# Orbit Chat

面向方形屏手表和小尺寸安卓设备的原生 Java 聊天客户端。支持 Android 8.0（API 26）及以上，可对接 OpenAI 兼容的 Chat Completions 与 Anthropic Messages。只使用平台 View、`HttpURLConnection`、SQLite 和私有 SharedPreferences，不依赖 Kotlin、Compose、AndroidX 或任何第三方网络库。

## 构建

- 工具链：AGP 8.7.3、Gradle 8.9、Android SDK Platform 35，`minSdk 26`、`targetSdk 28`，使用 JDK 17。
- 仓库未包含 `gradle-wrapper.jar`。装好 Gradle 后先执行 `gradle wrapper --gradle-version 8.9`，再运行 `gradlew.bat :app:assembleDebug`（Windows）或 `./gradlew :app:assembleDebug`。
- Java 源码已用 JDK 25 对 Android 35 的 `android.jar` 独立编译通过；Gradle 打包和真机运行尚未验证。

## 接口与验收

包名与接口约定、供应商端点形式、线程要求、设备验收矩阵见 [CONTRACT.md](CONTRACT.md)。主源码位于 `app/src/main/java/com/example/watchchat/`。
