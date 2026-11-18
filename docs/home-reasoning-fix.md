# 首页思考控件修复

用户模型：deepseek-flash。界面要求英文，不加入离线知识库以避免体量增加。

官方文档 https://api-docs.deepseek.com/zh-cn/guides/thinking_mode/ 实读：OpenAI Chat Completions 使用 thinking.type=enabled，reasoning_effort=low/high/max，默认high。首页显示LOW / HIGH / MAX并保留蓝紫左向粒子。第三方接口能否接受参数需服务商支持。

原实现限定少数官方模型与host导致隐藏；本次补充精确deepseek-flash匹配并按用户最新要求调整到首页消息输入栏左上方。

在线知识库：用户否决维基百科（国内访问）和离线库（体量）。候选 https://oiapi.net/api/BaiduEncyclopedia 对Python与地球测试均返回code -2及未收录，未验证有效，不接入。先移除旧SearXNG配置入口与启用路径，用户最终确认不再折腾知识库，本次不接入在线或离线知识库。
