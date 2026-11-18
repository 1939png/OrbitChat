# 贡献指南 / Contributing Guide

## 开始前 / Before you start

1. 阅读 ARCHITECTURE.md 和根目录 CONTRACT.md。
2. 确认改动属于 core、ui、data 或文档/模板，并记录线程边界。
3. 保持 Java、平台 Android Views、minSdk 26；不引入 AndroidX、Compose、Kotlin 或新的运行时网络库。
4. 中文说明同时写英文 API 名，便于维护和搜索。

## 修改原则 / Change principles

- 小步修改，保持 provider payload 和现有行为兼容。
- 网络、SQLite、SharedPreferences 磁盘操作不能跑在主线程。
- API key 不写日志、不进入导出文本、不放入示例输出。
- 不支持的 Markdown/数学表达式保留原文；流式响应保持顺序；取消保留部分文本。
- 手表和紧凑设备优先：内容可滚动，控件可触摸，避免固定宽度截断。
- 文档/模板改动不应修改业务源码；宿主适配点要明确。

## 流程 / Workflow

    inspect → focused change → narrow check → diff review → document API impact

1. 从 app/src/main/java/com/example/watchchat/ 的真实实现确认签名。
2. 先写行为不变量，例如 ChatClient callbacks remain on worker thread。
3. 运行对应独立测试或静态检查，再考虑完整 APK 构建。
4. 检查路径、包名、minSdk、类名和文档一致。
5. PR 描述写明改动、验证命令、未验证项和 API 影响。

## 测试 / Testing

    javac -encoding UTF-8 -d OUT app/src/main/java/com/example/watchchat/MessageFormatCore.java app/src/test/java/com/example/watchchat/MessageFormatCoreTest.java
    java -cp OUT com.example.watchchat.MessageFormatCoreTest
    gradlew.bat :app:assembleDebug

按 CONTRACT.md 验证 provider root、/v1、完整 endpoint、SSE 顺序、错误重试、取消、SessionRepository 的 %, _ 和反斜杠字面匹配；设备覆盖 API 26、当前 Android、240x240/320x320 方屏、360x640 紧凑手机。

## 提交清单 / Submission checklist

- [ ] 中文说明含英文 API 名称。
- [ ] 未引入 AndroidX、Compose、Kotlin 或运行时网络依赖。
- [ ] 网络、数据库、偏好存储不在主线程。
- [ ] 未泄露 API key。
- [ ] 已运行对应测试并记录结果。
- [ ] 文档、模板和示例可从干净 checkout 复现。
- [ ] PR 含行为变化、兼容性、证据和未验证项。
