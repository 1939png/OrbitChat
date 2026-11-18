# 文件与会话统计扩展说明

> 本文以当前源码为基线，明确区分当前已落地和规划扩展。规划项只描述边界、数据来源和兼容规则，不把尚未实现的接口写成现有 API。

## 当前实现基线

| 能力 | 当前源码事实 | 现状含义 |
| --- | --- | --- |
| 文件选择器 | SettingsActivity 使用 ACTION_OPEN_DOCUMENT、CATEGORY_OPENABLE 和 image/* | 目前只选择壁纸；返回 Uri 后通过 ContentResolver.openInputStream() 复制到 getFilesDir()/appearance/wallpaper。 |
| 私有文件 | AppearancePreferences.wallpaperFile() 位于应用私有 files 目录 | 私有文件不应被插件或导出文本直接当作可访问路径；当前没有通用附件仓库。 |
| 系统/共享目录 | 源码没有直接枚举或写入公共目录的实现 | 不能把系统文件路径、媒体库路径或真实路径当成现有契约。 |
| 对话输出 | MainActivity.shareCurrent() 通过 ACTION_SEND 分享 text/plain | 当前分享只包含内存中的消息文本；没有 ACTION_CREATE_DOCUMENT 文件落盘流程，也没有附件导出。 |
| 会话持久化 | SessionRepository 使用 chat_sessions.db，版本为 1，包含 sessions、messages 和消息时间索引 | API 是同步 SQLite API，调用者必须放在 worker thread；onUpgrade() 当前为空。 |
| Token 统计 | ChatClient 构造请求并消费流式内容，但当前没有从响应读取 usage，也没有 token 字段或统计表 | 现有 UI、导出和数据库都不应声称有精确 token 统计。 |

## 文件选择器与目录边界

### 选择器策略（规划）

新增通用附件导入时，宿主应优先使用系统文档选择器的持久化 content:// 授权：

- 单文件导入可沿用 ACTION_OPEN_DOCUMENT + CATEGORY_OPENABLE；多文件导入再增加 EXTRA_ALLOW_MULTIPLE。
- MIME 类型应由功能决定；未知类型使用可读的通配策略，并以实际 ContentResolver 元数据为准。
- 导入记录保存可重新打开所需的内容标识和展示元数据，而不是依赖临时绝对路径。
- 读取应通过 ContentResolver 流式完成；大文件、不可读 URI、权限失效和取消都要产生可观察的失败状态。
- 壁纸仍是现有专用流程。通用附件扩展不应复用壁纸文件名或覆盖壁纸目录。

### 私有目录与系统目录

- 应用私有目录（filesDir、cacheDir）适合保存应用管理的副本、缩略图和临时文件。私有目录路径不应暴露给插件、消息文本或导出内容。
- 系统文档提供者返回的 URI 可能对应本地、云端或可移动存储。调用方应把 URI 当作能力句柄，不能假设其有稳定本地路径，也不能通过字符串截取推断文件位置。
- 需要用户选择输出位置时，规划使用系统创建文档流程（ACTION_CREATE_DOCUMENT）；当前源码没有该流程。
- 私有副本的生命周期、清理策略、大小上限和失败重试应由宿主统一管理，插件只消费宿主授予的附件能力。

## 附件兼容降级

附件能力需要按运行环境和 provider 能力逐级降级，且每级结果可解释：

1. 原始附件：provider 和请求通道都支持附件时，传递原始 URI 的读取结果及 MIME/文件名元数据。
2. 应用内副本：provider 不接受 URI 句柄时，先读取到应用私有副本，再由宿主按该 provider 的现有请求格式适配。这个适配目前尚未实现。
3. 文本提取：仅当宿主已有对应解析器并成功提取时，降级为文本；文本应标记来源文件和截断状态。
4. 可见占位：读取失败、权限失效、类型不支持或大小超限时，保留用户可见的文件名/错误状态，不伪装成已发送附件。
5. 无附件继续：附件是可选输入时，失败项不能阻塞普通文本消息；消息上下文应能区分未选择、选择后失败和成功导入。

上述是扩展行为约束，不代表当前已有附件 API。当前唯一已落地的文件输入是壁纸图片复制。

## Token 统计：来源与未知值规则

### 来源优先级（规划）

统计字段应记录来源，避免把估算值误报为 provider 精确值：

1. provider 响应中的 usage 字段（若非流式响应或 provider 在流结束事件中提供）是精确来源；
2. provider 的流式 usage 事件（若协议和实现明确支持）是精确来源；
3. 本地 tokenizer 估算是估算来源，必须带估算标记和算法版本；
4. 没有可靠来源时为未知。

当前 ChatClient 只消费 OpenAI-compatible 的内容增量和 [DONE]，以及 Anthropic 的文本增量；源码没有读取 usage，因此当前统计值应为未知，而不是按字符数或请求长度猜测。

- input、output、total 三类值分别表示输入、输出和合计；
- 每个值都允许已知、估算、未知状态；未知值用 null 语义表达，避免用 0 混淆确实为零；
- total 只有在输入和输出都可可靠相加时才填精确值；否则保留未知或明确标为估算；
- 取消、网络错误、解析错误和仅收到部分流时，已收到的文本可持久化，但 token 统计应标记不完整；
- provider 返回的 usage 与本地估算冲突时，以 provider usage 为准，并保留来源字段；
- 展示层应显示未知或估算，导出层应携带来源/完整性标记，不能把未知渲染为 0。

## 数据库迁移

当前数据库版本是 1，且 onUpgrade 为空。任何会话统计、附件元数据或消息 token 字段都必须通过显式版本迁移加入：

1. 先提高 SQLiteOpenHelper 的数据库版本；
2. 在 onUpgrade(db, oldVersion, newVersion) 中按版本号逐步执行幂等迁移；
3. 新列允许旧数据为空，历史记录的统计值按未知处理；
4. 新表和索引应在同一迁移步骤创建，避免先写入后缺索引；
5. 迁移失败由 SQLite 事务回滚，不能静默吞掉异常；
6. 保持旧的会话、消息、搜索、删除和纯文本导出行为兼容；
7. 为每个版本覆盖空库创建、v1 升级、重复打开和旧数据读取测试。

具体表名、列名和公开 facade 需在实现时先确定；本文不把规划字段写成当前 schema。插件不得依赖内部 SQLite 表结构。

## 插件扩展点

当前插件接口包括 ChatPlugin、PluginMetadata、PluginRegistry、PluginContext、MessageContext、MessageHandler、ModelProvider 和 PluginResult。当前 PluginContext 只有 log(String)，因此插件目前没有文件、附件、统计或数据库访问能力。

未来扩展应遵循以下边界：

- 由宿主向 PluginContext 或独立 facade 增加稳定的附件读取能力；插件接收受限的内容句柄/元数据，不接触 Activity、绝对路径、API key 或 SQLite 连接。
- 由宿主向消息上下文提供只读会话标识和统计快照；插件不能自行推断 token，也不能覆盖 provider usage。
- 文件导入、输出和统计写入由宿主统一调度到 worker thread；插件回调只声明需求或消费结果。
- 插件声明能力时使用 PluginMetadata.attributes 的兼容扩展方式，未知 capability 由宿主忽略或返回清晰的 not-supported 结果；当前没有自动发现机制，仍需显式注册。
- 需要持久化的插件数据走未来的宿主 PluginStore facade；不要直接依赖 chat_sessions.db 的内部 schema。当前文档中的 PluginStore 只是既有指南所述演进方向，尚未实现。

一个可验收的未来插件扩展应至少说明：支持的 MIME 类型、最大输入大小、是否允许多选、输出格式、统计来源、未知值处理、线程要求、取消行为和宿主不支持时的降级结果。

## 自检清单

- [ ] 文档中的当前内容能在源码中定位；规划内容明确标注。
- [ ] 没有把 ACTION_CREATE_DOCUMENT、附件 API、usage 字段、统计表或 PluginStore 写成已经存在。
- [ ] 私有目录、系统 URI 和临时权限没有被混为绝对文件路径。
- [ ] 未知 token 值不会以 0 表示，估算值带来源和完整性语义。
- [ ] 数据库版本升级有明确迁移入口、回滚和旧数据规则。
- [ ] 插件扩展只经过宿主边界，不直接访问 Activity、SQLite、API key 或未公开实现。
