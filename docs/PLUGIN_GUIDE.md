# 插件开发指南 / Plugin Guide

## 当前状态

当前源码已有 ChatClient、ModelCapabilities、MessageFormatCore、AppearancePreferences、SessionRepository，以及 com.example.watchchat.plugin 下的 ChatPlugin、PluginMetadata、PluginRegistry、PluginContext、MessageContext、MessageHandler、ModelProvider、ModelRequest、PluginResult 和 PluginErrorHandler。plugin-template 是可复制的独立示例，接入时应遵循这些真实接口。插件不要直接引用 Activity、内部 SQLite schema 或未公开实现。

## 插件边界 / Plugin boundary

- ChatPlugin：通过 getMetadata() 提供元数据，可选实现 onInstall/onStart/onStop、getMessageHandler 和 getModelProvider。
- PluginMetadata：不可变 id、name、version、description 与 attributes。
- PluginContext：当前宿主暴露的窄上下文（log）。
- MessageContext：消息会话 id 和插件上下文。
- PluginResult：消息处理结果（handled/notHandled）。
- PluginRegistry：线程安全注册、启动/停止、卸载、消息分发、模型提供者枚举和错误隔离。

模板采用纯 Java 8，不依赖 AndroidX、Compose 或第三方库。

## 完整步骤 / End-to-end

### 1. 复制模板 / Copy

    mkdir -p app/src/main/java/com/example/watchchat/plugin/example
    cp -R docs/plugin-template/src/main/java/com/example/watchchat/plugin/template/* app/src/main/java/com/example/watchchat/plugin/example/

Windows PowerShell：

    Copy-Item -Recurse docs\plugin-template\src\main\java\com\example\watchchat\plugin\template\* app\src\main\java\com\example\watchchat\plugin\example\

替换包名、插件 id、显示名和逻辑；示例包名只是模板隔离用途。

### 2. 实现接口 / Implement

    public final class GreetingPlugin implements ChatPlugin {
        private static final PluginMetadata META = new PluginMetadata(
            "example.greeting", "Greeting", "1.0.0", "Replies to /hello");
        public PluginMetadata getMetadata() { return META; }
        public MessageHandler getMessageHandler() {
            return (context, input) -> input != null && input.startsWith("/hello")
                ? PluginResult.handled("Hello, " + input.substring(6).trim())
                : PluginResult.notHandled();
        }
    }

### 3. 注册插件 / Register

当前没有自动发现机制，先显式注册并启动：

    PluginRegistry registry = new PluginRegistry(context, errorHandler);
    registry.register(new GreetingPlugin());
    registry.startAll();
    PluginResult result = registry.dispatchMessage(messageContext, "/hello Orbit");

宿主应在应用级创建 registry，并在生命周期中调用 startAll()/stopAll()。重复 id 注册返回 false；插件回调异常由 PluginErrorHandler 隔离。若插件需要聊天能力，优先实现 ModelProvider，由宿主决定如何适配 ChatClient；不要向插件暴露 API key 或 HttpURLConnection。

### 4. 构建 / Build

    javac -encoding UTF-8 -d OUT docs/plugin-template/src/main/java/com/example/watchchat/plugin/template/*.java
    gradlew.bat :app:assembleDebug

复制到 app/src/main/java 后，当前 app/build.gradle 会自动编译 Java 源码，无需新增依赖。

### 5. 测试 / Test

    PluginRegistry registry = new PluginRegistry(context, PluginErrorHandler.NO_OP);
    assert registry.register(new GreetingPlugin());
    assert !registry.register(new GreetingPlugin());
    assert registry.listMetadata().size() == 1;
    assert registry.dispatchMessage(messageContext, "/hello Orbit").isHandled();
    assert !registry.dispatchMessage(messageContext, "/unknown").isHandled();

至少覆盖成功处理、未处理消息、重复注册、启动/停止生命周期、插件异常隔离、消息上下文和 UI 主线程调度。调用现有类时验证 ChatClient worker-thread callbacks、SessionRepository worker-thread 约束、ModelCapabilities 能力判断和 MessageFormatCore 显示边界。

### 6. 提交 / Submit

检查 id、能力、线程模型、宿主适配点；清除 API key、用户对话、私有 URL；确认无 AndroidX/Compose/Kotlin/运行时网络依赖；记录构建测试结果和未执行的设备验证。PR 同时写中文说明和英文 API 名。

## 现有模块接入建议

- ChatClient：插件通过宿主提供的 ModelProvider/adapter 使用聊天能力；宿主负责 ChatConfig、认证、SSE、取消。
- ModelCapabilities：宿主用 ModelCapabilities.forConfig(...) 检查模型能力，插件不复制 allowlist。
- MessageFormatCore：插件返回纯文本或支持的 Markdown 子集，UI 负责显示解析。
- AppearancePreferences：插件不直接访问外观偏好，未来由 PluginContext 提供主题 token。
- SessionRepository：插件不直接操作 SQLite，未来通过 PluginStore facade，并遵守 worker-thread。

## 版本 / Versioning

插件 id 发布后保持稳定；新增 capability 采用兼容方式。未知 capability 返回清晰错误。接口变化优先增加版本或 adapter，避免强制所有插件同时迁移。

## 文件与统计能力

插件当前没有文件选择器、附件、token 统计或 SQLite 访问能力；PluginContext 目前只提供 log(String)。宿主侧的扩展边界、URI/私有目录规则、附件降级、未知统计值和数据库迁移约束见 [file-and-session-stats.md](file-and-session-stats.md)。插件应等待宿主提供稳定 facade 后再接入，不应自行读取 chat_sessions.db 或推断 token。
