# Orbit Chat 架构说明

> 本文基于当前源码整理，现状与演进目标分开描述。

## 当前工程事实

- 单模块 Android 应用：app/，命名空间和 applicationId 都是 com.example.watchchat。
- Java 原生实现：平台 Android Views、HttpURLConnection、SQLite、私有 SharedPreferences。
- compileSdk 35、minSdk 26、targetSdk 28；Java 8 source/target 兼容；无 Kotlin、AndroidX、Compose 或运行时网络依赖。
- 目标设备为 1:1 手表和紧凑型 Android 设备；布局应可滚动，磁盘和网络操作放在工作线程。

## 当前职责与 API

| 当前位置 | 职责 | English API |
| --- | --- | --- |
| MainActivity | 聊天页面、会话切换、发送/停止和 UI 调度 | MainActivity / screen controller |
| SettingsActivity | Provider、模型、外观和搜索设置 | SettingsActivity / settings screen |
| data/ChatClient | OpenAI-compatible Chat Completions 与 Anthropic Messages 流式 HTTP | ChatClient.send(...) / streaming client |
| data/ModelCapabilities | 依据 provider、官方 host、model 判断 reasoning/thinking 能力 | ModelCapabilities.forConfig(...) / capability resolver |
| data/SessionRepository | SQLite 会话、消息、搜索、导出、删除；同步 API | SessionRepository / persistence repository |
| MessageFormatCore | 显示用 Markdown 子集、表格、代码围栏和有限数学解析 | MessageFormatCore.parse(...) / display formatter core |
| AppearancePreferences | 外观 SharedPreferences 和私有壁纸文件 | AppearancePreferences / appearance preferences |
| MessageFormatter、AssistantMessageView | 将格式化结果渲染为平台 View | renderer |

ChatClient 回调在 worker thread；UI 更新必须切回主线程。SessionRepository 是同步 API，所有调用必须在 worker thread。CancelHandle 断开活动请求；流式文本按顺序追加，完成、取消和终端错误保留已收到的部分文本。

## 数据流

    ChatConfigStore → MainActivity → ChatClient.send(history, config, options, listener)
      → provider SSE → ChatListener → main thread View → SessionRepository.appendMessage

外观由 AppearancePreferences 独立保存；消息经 MessageFormatCore、MessageFormatter、AssistantMessageView 显示。ModelCapabilities 集中负责能力判断，UI 不应复制 provider allowlist。

## 边界规则

1. UI boundary：Activity、View、listener 的 UI 更新只在主线程。
2. Network boundary：ChatClient 负责 endpoint、认证头、SSE、重试、取消；API key 不写日志或导出文本。
3. Persistence boundary：SessionRepository 提供同步 SQLite API，调用者负责线程调度。
4. Capability boundary：能力由 ModelCapabilities.forConfig 得出；未知配置返回空能力集。
5. Rendering boundary：MessageFormatCore 是 display-only parser；不支持的公式保留原文。
6. Device boundary：最低 API 26；高版本能力用版本判断保护。

## 逐步拆分 core / ui / data

无需一次迁移，按调用链逐步进行：

1. **整理清单**：标记无 Android 依赖的消息模型、请求选项、能力值校验、格式化规则为 core 候选，先不改调用点。
2. **抽出 core**：移动纯 Java 类型和规则；core 不引用 Activity、View、Context、SQLite、HttpURLConnection。保留 facade 兼容旧入口。
3. **收拢 data**：ChatClient、ChatConfigStore、SessionRepository、provider 适配和 SQLite 留在 data；为 ChatGateway、SessionStore 增加接口，再替换 Activity 的具体类型依赖。
4. **收拢 ui**：Activity、View、渲染器和外观适配归入 ui；UI 只依赖 core 与 data facade，不拼 provider JSON、不执行 SQLite。
5. **建立插件接缝**：core 放稳定元数据/能力声明，data 提供注册存储，ui 决定入口；每次迁移都执行构建、测试和 API 26 静态检查。

目标依赖方向为 ui → core、ui → data facade，data → core，core 不反向依赖 UI 或 Android framework。当前源码尚未完成物理分包，这些是演进目标。

## 验证

    javac -encoding UTF-8 -d OUT app/src/main/java/com/example/watchchat/MessageFormatCore.java app/src/test/java/com/example/watchchat/MessageFormatCoreTest.java
    java -cp OUT com.example.watchchat.MessageFormatCoreTest
    gradlew.bat :app:assembleDebug

README.md 和 CONTRACT.md 已记录 Java 独立编译已验证，Gradle APK、设备和网络验收仍需对应环境。

## 文件、附件与会话统计扩展

文件选择、附件兼容降级、token 统计、数据库迁移和插件边界的当前事实与规划见 [file-and-session-stats.md](file-and-session-stats.md)。该文明确区分已落地实现与待实现扩展，避免把规划 API 当成当前契约。
