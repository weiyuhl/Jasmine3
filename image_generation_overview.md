# 图像生成功能总览（本地项目）

- 触发路径：页面点击“生成”→ `ImgGenVM.generateImage` 读取设置与输入→ 路由到 Provider 执行 HTTP 生成→ 返回后写入本地存储与数据库→ UI 预览当前批次并在图库页分页展示历史记录。
- 核心参与者：`ImageGenPage`（UI）→ `ImgGenVM`（业务）→ `ProviderManager`（Provider 路由）→ `OpenAIProvider`/`GoogleProvider`/`ClaudeProvider`（HTTP 实现）→ 本地文件与 Room（落盘与索引）。
- 可定制项：模型选择、生成数量、宽高比 `ImageAspectRatio`、以及每模型的 `customHeaders/customBodies`（附加服务商参数）。

## 核心接口与数据
- Provider 接口：统一的图片生成能力 `generateImage`（ai/src/main/java/com/lhzkml/ai/provider/Provider.kt:34-37）。
- 参数模型：`ImageGenerationParams(model, prompt, numOfImages, aspectRatio, customHeaders, customBody)`（ai/src/main/java/com/lhzkml/ai/provider/Provider.kt:53-60）。
- 结果模型：`ImageGenerationResult(items: List<ImageGenerationItem>)`；单项含 `data`（Base64/可能含 data 前缀）与 `mimeType`（ai/src/main/java/com/lhzkml/ai/ui/Image.kt:6-14）。
- 宽高比枚举：`ImageAspectRatio` 支持 `SQUARE/LANDSCAPE/PORTRAIT`（ai/src/main/java/com/lhzkml/ai/ui/Image.kt:17-21）。
- 模型扩展：`Model` 支持类型 `ModelType.IMAGE`、`customHeaders`、`customBodies` 以及 `providerOverwrite`（ai/src/main/java/com/lhzkml/ai/provider/Model.kt:8-20, 23-27）。

## Provider 实现
- OpenAI（DALL·E/GPT-Image 系列）
  - 路径：`/images/generations`，`response_format=b64_json`，宽高比映射为具体尺寸（`1024x1024`、`1536x1024`、`1024x1536`），支持 `n` 张图（ai/src/main/java/com/lhzkml/ai/provider/providers/OpenAIProvider.kt:141-196）。
  - 认证与自定义：`Authorization: Bearer <key>`；支持 `customHeaders` 与 `customBody` 合并（ai/src/main/java/com/lhzkml/ai/provider/providers/OpenAIProvider.kt:167-173, 151-166）。
- Google（Gemini Image via PaLM/Gemini 预测接口）
  - 路径：`...:predict`，Vertex 与普通 API 自动选择；请求体 `instances` 与 `parameters`（含 `sampleCount` 与 `aspectRatio` 映射 `1:1/16:9/9:16`）（ai/src/main/java/com/lhzkml/ai/provider/providers/GoogleProvider.kt:679-747）。
  - 认证与改造：通过 `transformRequest` 与 `configureReferHeaders` 注入必要头信息，支持代理（ai/src/main/java/com/lhzkml/ai/provider/providers/GoogleProvider.kt:712-724）。
  - 解析：从 `predictions[].bytesBase64Encoded` 构造 `ImageGenerationItem`（ai/src/main/java/com/lhzkml/ai/provider/providers/GoogleProvider.kt:732-744）。
- Claude（不支持图片生成）：直接抛错（ai/src/main/java/com/lhzkml/ai/provider/providers/ClaudeProvider.kt:91-96）。
- Provider 管理与路由：`ProviderManager.getProviderByType(setting)` 将 `ProviderSetting.OpenAI/Google/Claude` 路由到实例（ai/src/main/java/com/lhzkml/ai/provider/ProviderManager.kt:48-55）。

## 业务层（ViewModel）
- 状态与分页：`prompt/numberOfImages/aspectRatio/isGenerating/error/currentGeneratedImages` 等 StateFlow；历史图片分页从 `GenMediaRepository` 取 `PagingSource` 并映射为 `GeneratedImage`（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:74-90, 82-88）。
- 触发生成：`generateImage()` 读取 `SettingsStore.settingsFlow.first()`，选择模型与 Provider 构造 `ImageGenerationParams`，调用 Provider 的 `generateImage`（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:107-137）。
- 写入与回显：遍历 `result.items`，`saveImageToStorage` 将 Base64 写入 `files/images`，插入数据库并更新 `_currentGeneratedImages`（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:139-171, 172-198）。
- 取消与删除：`cancelGeneration()` 取消当前 Job；`deleteImage(image)` 先删 DB，再删文件（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:168-171, 200-216）。
- 约束与更新：生成数量限制 1–4（`updateNumberOfImages`），宽高比更新（`updateAspectRatio`），清理错误（`clearError`）（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:95-101, 103-106）。

## UI 交互
- 顶层页面：`ImageGenPage` 两页分页（生成/图库），底部导航切换；生成页支持取消生成对话框（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:102-151）。
- 生成页：显示当前批次生成的最多两张预览图、输入栏（设置按钮、提示输入、生成/取消按钮），错误通过 Toast 呈现（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:214-293）。
- 输入栏触发：`FilledTonalIconButton` 中点击生成/取消，调用 `vm.generateImage()/vm.cancelGeneration()`（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:325-333）。
- 设置底板：选择 `ModelType.IMAGE` 的模型、生成数量、宽高比（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:521-611）。
- 图库页：分页列表展示历史生成图片，支持“复制 prompt”“保存到系统（导出）”“删除图片”，可点击放大预览（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:349-519）。

## 数据落盘与索引
- 文件存储：`getImagesDir()` 返回 `files/images`；`createImageFileFromBase64(base64, path)` 写入 PNG 文件（app/src/main/java/com/lhzkml/jasmine/utils/ChatUtil.kt:234-240, 242-254）。
- Room 仓库与 DAO：`GenMediaRepository` 封装 `insert/delete/getAll`；DAO 定义 `PagingSource` 与删除接口（app/src/main/java/com/lhzkml/jasmine/data/repository/GenMediaRepository.kt:7-13；app/src/main/java/com/lhzkml/jasmine/data/db/dao/GenMediaDAO.kt:10-21）。
- 实体结构：`GenMediaEntity(path, modelId, prompt, createAt)`，`id` 自增主键（app/src/main/java/com/lhzkml/jasmine/data/db/entity/GenMediaEntity.kt:7-18）。
- VM 中实体映射：将 DB 中相对路径 `images/<name>` 转换为绝对路径用于展示（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:43-54）。

## 设置来源与模型选择
- 设置读取：`SettingsStore.settingsFlow` 提供当前模型与 Provider 列表；`imageGenerationModelId` 指向当前选择的图片模型（app/src/main/java/com/lhzkml/jasmine/data/datastore/PreferencesStore.kt:121-127）。
- 模型查找与 Provider 解析：`Settings.findModelById` 找模型；`Model.findProvider(settings.providers)` 解析到具体 `ProviderSetting`（app/src/main/java/com/lhzkml/jasmine/data/datastore/PreferencesStore.kt:339-351, 367-374）。
- UI 中模型筛选：`ModelSelector(type = ModelType.IMAGE)` 仅展示图片能力模型（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:550-559）。

## 扩展与定制
- 自定义头与请求体：`CustomHeader` 转 `okhttp3.Headers`（`toHeaders`），`CustomBody` 合并到 JSON（递归合入 `mergeCustomBody`），便于对不同服务商追加私有参数（ai/src/main/java/com/lhzkml/ai/util/Request.kt:14-22, 48-68, 73-89）。
- Referer/标题注入：`configureReferHeaders` 针对特定域名注入标识头（Google 路径已使用）（ai/src/main/java/com/lhzkml/ai/util/Request.kt:24-39；ai/src/main/java/com/lhzkml/ai/provider/providers/GoogleProvider.kt:720-722）。

## 异常与并发
- 并发控制：`viewModelScope.launch` + `cancelJob`；重复点击“生成”会先取消上一次任务（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:107-111, 168-171）。
- 错误反馈：捕获异常并通过 `_error` 触发 Toast 展示（app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenVM.kt:160-164；app/src/main/java/com/lhzkml/jasmine/ui/pages/imggen/ImgGenPage.kt:230-235）。
- Provider 级错误：HTTP 非 2xx 报错并附带响应内容，便于诊断（OpenAI：ai/src/main/java/com/lhzkml/ai/provider/providers/OpenAIProvider.kt:175-181；Google：ai/src/main/java/com/lhzkml/ai/provider/providers/GoogleProvider.kt:724-731）。

