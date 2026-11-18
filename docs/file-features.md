# 文件功能

发送按钮旁的“＋”使用系统 ACTION_OPEN_DOCUMENT，无需存储权限，可选择相册照片或 UTF-8 文本文件。文本内容会以内嵌 <attachment_text> 进入现有 ChatClient 历史；图片保存 URI、MIME 与文件名，并以可审计附件标记进入请求。当前 ChatClient 的 OpenAI/Anthropic 载荷仍是字符串 content，因此图片采用明确的兼容文本降级，后续接入可靠多模态接口时只需扩展 Attachment 到 payload 层。

“保存回答”可保存全部代码块为 .md 或整条回答为 .txt。默认写入应用私有 files/exports 目录；设置中打开“保存回答时使用系统目录”后调用 ACTION_CREATE_DOCUMENT。
