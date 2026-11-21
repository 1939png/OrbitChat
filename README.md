# Orbit Chat

Orbit Chat 是面向方形屏手表和小尺寸安卓设备的原生 Java 聊天客户端，支持 Android 8.0+，可对接 OpenAI 兼容的 Chat Completions 与 Anthropic Messages，不依赖 Kotlin、Compose、AndroidX 或第三方网络库。

项目把聊天界面、网络请求、会话存储和配置管理放在一个 Android 模块里。界面使用平台 Activity 和 View，网络使用 HttpURLConnection，会话写入 SQLite，配置与外观保存在应用私有 SharedPreferences。目标设备是 1:1 方形手表和内存紧张的紧凑型安卓设备，布局需要滚动，触控目标按小屏操作设计。

聊天请求支持 SSE 流式返回。发送后可以主动停止，停止、完成或遇到错误时，已经收到的文字会保留下来。OpenAI 兼容接口使用 Bearer 认证，Anthropic 使用 x-api-key 与 anthropic-version，系统消息按 Anthropic 的顶层 system 字段发送。遇到 401 和 400 会直接报错，429、5xx 和暂时性网络错误按配置重试。API key 只保存在应用私有 SharedPreferences，不写日志，也不进入导出的聊天文本。

会话侧提供新建、切换、删除、搜索和导出。搜索把 %、_ 和反斜杠当作普通字符处理，不把它们当 SQL 通配符。数据库接口是同步的，调用方需要放在工作线程，界面回调再切回主线程。

显示层支持 Markdown 子集、表格、代码围栏和有限数学解析，遇到不支持的公式保留原文。模型是否支持 reasoning 或 thinking，由 ModelCapabilities 根据 provider、官方 host 和模型名判断，界面不复制 provider 白名单。

插件接口位于 com.example.watchchat.plugin。插件可以注册消息处理器或模型提供者，PluginRegistry 负责注册、启动、停止、分发和错误隔离。仓库里有可复制的 plugin-template，以及中英文的接入说明。

## 目录

- `app/`：Android 应用与 Java 源码。
- `CONTRACT.md`：原生 Android 集成契约、接口和验收项。
- `docs/ARCHITECTURE.md`：模块职责、数据流和拆分方向。
- `docs/PLUGIN_GUIDE.md`：插件接口、注册步骤和测试方式。
- `docs/plugin-template/`：纯 Java 8 的插件模板。

## 构建

需要 JDK 17、Gradle 8.9、Android SDK Platform 35 和 AGP 8.7.3。

```bat
gradlew.bat :app:assembleDebug
```

Java 源码的独立编译已经验证，Gradle APK 组装、真机运行和网络验收还需要在对应环境里完成。验收清单写在 `CONTRACT.md`。

## 适用场景

这个仓库适合作为方形手表聊天客户端的基础实现，也适合查看原生 Java 在流式网络、SQLite 会话和小屏 View 布局上的组织方式。代码刻意避开 Kotlin、Compose、AndroidX 和第三方网络库，便于在低内存设备上控制依赖。
