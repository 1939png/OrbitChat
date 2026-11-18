# 插件模板 / Plugin Template

这是可复制的纯 Java 8 插件骨架，演示 Plugin、PluginContext、PluginResult、PluginRegistry、ExamplePlugin。不修改 Orbit Chat 业务代码，也不假设当前已有插件宿主。

## 使用

    cp -R docs/plugin-template/src/main/java/com/example/watchchat/plugin/template app/src/main/java/com/example/watchchat/plugin/

1. 修改 ExamplePlugin 的 id()、displayName()、capabilities()、execute(...)。
2. 宿主适配层创建 PluginRegistry 并显式 register(...)。
3. 写成功、空输入、未知 id、重复注册测试。
4. 编译模板并构建 app。

    javac -encoding UTF-8 -d OUT src/main/java/com/example/watchchat/plugin/template/*.java
    gradlew.bat :app:assembleDebug

PluginContext 只暴露最小宿主能力；没有 API key、Activity、View、SQLite 或网络对象。耗时工作交给宿主 worker executor。宿主应在应用级注册一次，并在禁用时调用 onUnload。
