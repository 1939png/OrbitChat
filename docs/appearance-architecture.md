# 个性化设置架构

个性化配置集中在 AppearancePreferences：默认值、SharedPreferences 键、主题模板、模糊级别与壁纸私有文件路径由此统一管理。SettingsActivity 只负责编辑和选择文件；VisualEffects 只负责将外观状态应用到 View；MainActivity 在启动和恢复时重新应用状态。

外观折叠框包含壁纸、动态模糊开关与强度、颜色模板、恢复默认。恢复默认只清理 appearance 配置和 files/appearance/wallpaper，不影响模型预设、聊天记录和消息数据库。

兼容性：壁纸通过系统文档选择器读取；RenderEffect 仅 API 31+，低版本跳过模糊。
