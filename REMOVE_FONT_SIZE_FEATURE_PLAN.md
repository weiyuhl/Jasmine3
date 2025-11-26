# 移除聊天字体大小控制功能方案

## 1. 目标
移除应用中的“聊天字体大小”调节功能，包括设置页面中的调节滑块、预览文本，以及聊天消息界面的字体缩放逻辑。

## 2. 影响范围分析

### 2.1 UI 界面
- **设置页面 (`SettingDisplayPage.kt`)**: 包含字体大小调节的 Slider 组件、百分比显示 Text 以及预览用的 `MarkdownBlock`。
- **聊天消息组件 (`ChatMessage.kt`)**: 读取 `fontSizeRatio` 并通过 `ProvideTextStyle` 应用到聊天内容。

### 2.2 数据存储
- **PreferencesStore (`PreferencesStore.kt`)**: `DisplaySetting` 数据类中包含 `fontSizeRatio` 字段，默认值为 1.0f。

### 2.3 资源文件
- **字符串资源 (`strings.xml`)**: 包含 `setting_display_page_font_size_title` 和 `setting_display_page_font_size_preview` 等相关文案。

## 3. 修改步骤

### 步骤 1: 移除 UI 设置入口
**文件**: `app/src/main/java/com/lhzkml/jasmine/ui/pages/setting/SettingDisplayPage.kt`
- 删除 `聊天字体大小` 标题所在的 `ListItem`。
- 删除包含 `Slider` 和百分比 `Text` 的 `Row` 组件。
- 删除用于预览的 `MarkdownBlock` 组件。

### 步骤 2: 移除聊天界面字体缩放逻辑
**文件**: `app/src/main/java/com/lhzkml/jasmine/ui/components/message/ChatMessage.kt`
- 找到 `ChatMessage` 函数中的 `textStyle` 定义：
  ```kotlin
  val textStyle = LocalTextStyle.current.copy(
      fontSize = LocalTextStyle.current.fontSize * settings.fontSizeRatio,
      lineHeight = LocalTextStyle.current.lineHeight * settings.fontSizeRatio
  )
  ```
- **关键修改**: 
  1. 删除上述 `textStyle` 变量定义。
  2. 删除包裹内容的 `ProvideTextStyle(textStyle) { ... }` 组件，将内部的 `MessagePartsBlock` 和 `CollapsibleTranslationText` 移到外层直接显示。
- **效果确认**: 移除缩放逻辑后，组件将直接继承系统默认的 `LocalTextStyle`，这等同于 `fontSizeRatio` 恒定为 `1.0f` 的效果，确保字体大小恢复并保持为标准大小。

### 步骤 3: 移除数据存储字段
**文件**: `app/src/main/java/com/lhzkml/jasmine/data/datastore/PreferencesStore.kt`
- 在 `DisplaySetting` 数据类中删除 `val fontSizeRatio: Float = 1.0f,` 字段。
- 如果有其他地方引用了 `DisplaySetting.fontSizeRatio`（如构建默认值或迁移逻辑），也一并移除。

### 步骤 4: 清理资源文件
**文件**: 
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- 以及其他可能存在的语言目录（如 `values-en`, `values-ja` 等）

- 删除以下字符串资源：
  - `<string name="setting_display_page_font_size_title">...</string>`
  - `<string name="setting_display_page_font_size_preview">...</string>`

## 4. 验证计划
1. **编译检查**: 确保所有引用 `fontSizeRatio` 的代码都已移除，项目编译无报错。
2. **设置页面检查**: 进入“显示设置”页面，确认“聊天字体大小”调节选项已消失。
3. **聊天页面检查**: 发送或查看聊天消息，确认字体显示大小正常（应为系统默认大小），且不再受之前的设置影响。
