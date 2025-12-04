# 移除翻译功能方案

目标：彻底移除应用内的翻译功能（聊天消息内的翻译与专用翻译器页面），包括 UI、ViewModel、Service、核心处理逻辑与设置项。

范围与步骤：

1. 聊天界面
- 移除消息操作中的“翻译”按钮与语言选择弹窗。
- 移除消息卡片内的译文折叠块渲染。
- 删除 `onTranslate` 与 `onClearTranslation` 回调链：`ChatList` → `ChatMessage` → `ChatMessageActionButtons`。

2. 专用翻译器页面
- 删除 `TranslatorPage.kt` 与 `TranslatorVM.kt` 文件。
- 在依赖注入模块中移除 `TranslatorVM` 的注册。

3. 服务与核心逻辑
- 在 `ChatService` 中删除 `translateMessage(...)`、`updateTranslationField(...)`、`clearTranslationField(...)`。
- 在 `GenerationHandler` 中删除 `translateText(...)`（含普通模型与 Qwen MT 分支）。

4. 设置项
- 在 `SettingModelPage` 中删除“翻译模型与提示词”设置卡片与默认提示词恢复。
- 在 `PreferencesStore` 中移除对 `translateModeId` 与 `translatePrompt` 的读取与写入；删除 `Settings` 数据类中的两个字段。

5. 文件删除
- `app/src/main/java/com/lhzkml/jasmine/ui/pages/translator/TranslatorPage.kt`
- `app/src/main/java/com/lhzkml/jasmine/ui/pages/translator/TranslatorVM.kt`
- `app/src/main/java/com/lhzkml/jasmine/ui/components/message/ChatMessageTranslation.kt`
- `app/src/main/java/com/lhzkml/jasmine/data/ai/prompts/Translation.kt`

6. 验证
- 执行构建：`./gradlew.bat app:assembleDebug`。
- 保留 `UIMessage.translation` 字段以保证历史数据兼容；如需进一步移除该字段及相关资源字符串，可在后续步骤中按需清理。

