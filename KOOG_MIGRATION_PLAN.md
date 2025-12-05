# Jasmine AI 模块迁移到 Koog 框架方案

## 1. 概述

### 1.1 目标
将 Jasmine 现有的自研 AI 模块（`ai/` 目录）替换为 JetBrains Koog 框架，以获得：
- 更强大的 Agent 能力（图工作流、状态管理）
- 更多 LLM 提供商支持（DeepSeek、Ollama、Bedrock 等）
- 企业级特性（OpenTelemetry、持久化、记忆）
- 更好的流式处理和结构化输出支持

### 1.2 当前架构分析

#### Jasmine 现有 AI 模块结构
```
ai/src/main/java/com/lhzkmlai/    # 注意：包名是 com.lhzkmlai（无点分隔）
├── core/                    # 核心抽象
│   ├── MessageRole.kt       # 消息角色枚举
│   ├── Reasoning.kt         # 推理相关（ReasoningLevel 枚举）
│   ├── Tool.kt              # 工具定义
│   └── Usage.kt             # Token 使用统计
├── provider/                # Provider 抽象层
│   ├── Provider.kt          # Provider 接口（无状态设计）
│   ├── ProviderSetting.kt   # Provider 配置（OpenAI/Google/Claude）
│   ├── ProviderManager.kt   # Provider 管理器
│   ├── Model.kt             # 模型定义（含 ModelType, Modality, ModelAbility, BuiltInTools）
│   └── providers/           # 具体实现
│       ├── OpenAIProvider.kt
│       ├── GoogleProvider.kt
│       ├── ClaudeProvider.kt
│       └── openai/          # OpenAI 子模块
│           ├── ChatCompletionsAPI.kt  # Chat Completions API 实现
│           ├── ResponseAPI.kt         # Response API 实现
│           └── OpenAIImpl.kt
├── ui/
│   └── Message.kt           # UI 消息模型（UIMessage, UIMessagePart, MessageChunk）
├── registry/
│   ├── ModelMatcher.kt
│   └── ModelRegistry.kt     # 模型匹配器（如 GEMINI_3_SERIES）
├── mnn/                     # 本地模型支持（MNN 框架）
│   ├── ChatService.kt
│   ├── ChatSession.kt
│   └── ...
└── util/                    # 工具类
    ├── KeyRoulette.kt       # API Key 轮询
    ├── ProxyUtils.kt        # 代理配置
    ├── Request.kt           # HTTP 请求
    └── ...
```

> **注意**：SSE 流处理是通过 OkHttp 的 `EventSource` 实现的，没有独立的 SSE.kt 文件。

#### Koog 框架核心结构
```
koog/
├── agents/                  # Agent 核心
│   ├── agents-core/         # AIAgent, Strategy, Environment
│   ├── agents-tools/        # Tool, ToolRegistry
│   ├── agents-mcp/          # MCP 集成
│   └── agents-features-*/   # 特性扩展
├── prompt/                  # LLM 交互层
│   ├── prompt-model/        # Prompt, Message 模型
│   ├── prompt-executor/     # PromptExecutor 抽象
│   └── prompt-executor-clients/  # LLM 客户端实现
│       ├── prompt-executor-openai-client/
│       ├── prompt-executor-anthropic-client/
│       ├── prompt-executor-google-client/
│       └── ...
└── http-client/             # HTTP 客户端抽象
```


## 2. 架构映射分析

### 2.1 核心概念映射

| Jasmine 现有概念 | Koog 对应概念 | 说明 |
|-----------------|--------------|------|
| `Provider<T>` | `LLMClient` | LLM 提供商抽�?|
| `ProviderSetting` | `LLMClient` 配置 | 提供商配置（API Key、Base URL 等） |
| `ProviderManager` | `PromptExecutor` | 执行器管理，Koog 支持�?LLM 编排 |
| `Model` | `LLModel` | 模型定义 |
| `UIMessage` | `Message` | 消息模型 |
| `UIMessagePart` | `Message.Content` | 消息内容部分 |
| `MessageRole` | `Message.Role` | 消息角色 |
| `Tool` | `Tool<TArgs, TResult>` | 工具定义 |
| `TextGenerationParams` | `PromptExecutor.execute()` 参数 | 生成参数 |
| `MessageChunk` | `StreamFrame` | 流式响应�?|
| `TokenUsage` | `ResponseMetaInfo.usage` | Token 使用统计 |

### 2.1.1 Jasmine 核心接口详细分析

#### Provider 接口
```kotlin
// 无状态设计，每次调用都需要传�?ProviderSetting
interface Provider<T : ProviderSetting> {
    suspend fun listModels(providerSetting: T): List<Model>
    suspend fun getBalance(providerSetting: T): String  // 默认返回 "TODO"
    suspend fun generateText(providerSetting: T, messages: List<UIMessage>, params: TextGenerationParams): MessageChunk
    suspend fun streamText(providerSetting: T, messages: List<UIMessage>, params: TextGenerationParams): Flow<MessageChunk>
}
```

#### ProviderSetting 抽象�?
```kotlin
// 注意：新�?Provider 类型需要实现所有抽象成�?
@Serializable
sealed class ProviderSetting {
    // 基础配置
    abstract val id: Uuid
    abstract val enabled: Boolean
    abstract val name: String
    abstract val models: List<Model>
    abstract val proxy: ProviderProxy
    abstract val balanceOption: BalanceOption

    // UI 相关（@Transient，不参与序列化）
    abstract val builtIn: Boolean
    abstract val description: @Composable() () -> Unit
    abstract val shortDescription: @Composable() () -> Unit

    // 模型管理方法
    abstract fun addModel(model: Model): ProviderSetting
    abstract fun editModel(model: Model): ProviderSetting
    abstract fun delModel(model: Model): ProviderSetting
    abstract fun moveMove(from: Int, to: Int): ProviderSetting
    abstract fun copyProvider(...): ProviderSetting
}
```

#### Model 数据模型
```kotlin
@Serializable
data class Model(
    val modelId: String = "",
    val displayName: String = "",
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,           // CHAT, IMAGE, EMBEDDING
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val inputModalities: List<Modality> = listOf(Modality.TEXT),   // TEXT, IMAGE
    val outputModalities: List<Modality> = listOf(Modality.TEXT),  // TEXT, IMAGE
    val abilities: List<ModelAbility> = emptyList(),  // TOOL, REASONING
    val tools: Set<BuiltInTools> = emptySet(),        // Search, UrlContext（Google 内置工具�?
    val providerOverwrite: ProviderSetting? = null,
)
```

#### TextGenerationParams
```kotlin
@Serializable
data class TextGenerationParams(
    val model: Model,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val tools: List<Tool> = emptyList(),
    val thinkingBudget: Int? = null,      // Reasoning 模式�?token 预算
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBody: List<CustomBody> = emptyList(),
)
```

#### UIMessage 数据模型
```kotlin
data class UIMessage(
    val id: Uuid,
    val role: MessageRole,           // SYSTEM, USER, ASSISTANT, TOOL
    val parts: List<UIMessagePart>,  // 支持多种内容类型
    val annotations: List<UIMessageAnnotation>,  // URL 引用�?
    val createdAt: LocalDateTime,
    val modelId: Uuid?,
    val usage: TokenUsage?
)

// 支持的消息部分类�?
sealed class UIMessagePart {
    data class Text(val text: String, var metadata: JsonObject?)
    data class Image(val url: String, var metadata: JsonObject?)
    data class Video(val url: String, var metadata: JsonObject?)
    data class Document(val url: String, val fileName: String, val mime: String, var metadata: JsonObject?)
    data class Reasoning(val reasoning: String, val createdAt: Instant, val finishedAt: Instant?, var metadata: JsonObject?)
    data class ToolCall(val toolCallId: String, val toolName: String, val arguments: String, var metadata: JsonObject?)
    data class ToolResult(val toolCallId: String, val toolName: String, val content: JsonElement, val arguments: JsonElement, var metadata: JsonObject?)
}
```

#### Tool 定义
```kotlin
data class Tool(
    val name: String,
    val description: String,
    val parameters: () -> InputSchema?,  // 延迟计算参数 schema
    val systemPrompt: (model: Model, messages: List<UIMessage>) -> String,  // 动态系统提�?
    val execute: suspend (JsonElement) -> JsonElement  // 执行函数
)
```

### 2.1.2 Koog 核心接口详细分析

#### LLMClient 接口
```kotlin
interface LLMClient {
    suspend fun execute(prompt: Prompt, model: LLModel, params: LLMParams? = null): String
    suspend fun executeStreaming(prompt: Prompt, model: LLModel, params: LLMParams? = null): Flow<StreamFrame>
    suspend fun executeWithStructuredOutput<T>(prompt: Prompt, model: LLModel, config: StructuredOutputConfig<T>): T
    suspend fun moderate(content: String): ModerationResult  // 部分客户端支�?
    suspend fun embed(texts: List<String>): List<List<Float>>  // 部分客户端支�?
}
```

#### Prompt �?Message 模型
```kotlin
// Prompt DSL 构建
val prompt = prompt {
    system("You are a helpful assistant")
    user("Hello")
    assistant("Hi there!")
    user {
        text("What's in this image?")
        image("/path/to/image.jpg")  // 多模态支�?
    }
}

// Message 类型
sealed class Message {
    data class System(val content: String)
    data class User(val content: List<Content>)  // 支持多模�?
    data class Assistant(val content: String, val toolCalls: List<ToolCall>?)
    data class Tool(val toolCallId: String, val content: String)
}
```

#### StreamFrame 类型
```kotlin
sealed class StreamFrame {
    data class Append(val text: String) : StreamFrame()  // 文本增量
    data class ToolCall(val id: String, val name: String, val args: String) : StreamFrame()  // 工具调用
    data class End(val finishReason: String?, val metaInfo: ResponseMetaInfo?) : StreamFrame()  // 结束�?
}
```

#### Tool 系统
```kotlin
// 方式1: 注解�?
class MyToolSet : ToolSet {
    @Tool
    @LLMDescription("Adds two numbers")
    fun add(@LLMDescription("First number") a: Int, @LLMDescription("Second number") b: Int): Int = a + b
}

// 方式2: Lambda �?
val tool = simpleTool(
    name = "search",
    description = "Search the web"
) { args: SearchArgs -> searchService.search(args.query) }

// 方式3: 类式
class SearchTool : Tool<SearchArgs, SearchResult> {
    override val name = "search"
    override val description = "Search the web"
    override suspend fun execute(args: SearchArgs, enabler: DirectToolCallsEnabler): SearchResult { ... }
}

### 2.2 Provider 类型映射

| Jasmine Provider | Koog Client | 模块 | 多模态支�?|
|-----------------|-------------|------|-----------|
| `ProviderSetting.OpenAI` | `OpenAILLMClient` | `prompt-executor-openai-client` | 图片�?音频�?文档�?|
| `ProviderSetting.Google` | `GoogleLLMClient` | `prompt-executor-google-client` | 图片�?音频�?视频�?文档�?|
| `ProviderSetting.Claude` | `AnthropicLLMClient` | `prompt-executor-anthropic-client` | 图片�?文档�?|
| - | `DeepSeekLLMClient` | `prompt-executor-deepseek-client` | 继承 OpenAI 基类 |
| - | `OllamaLLMClient` | `prompt-executor-ollama-client` | 图片�?|
| - | `OpenRouterLLMClient` | `prompt-executor-openrouter-client` | 图片�?音频�?文档�?|
| - | `BedrockLLMClient` | `prompt-executor-bedrock-client` | 取决于底层模�?|
| - | `MistralAILLMClient` | `prompt-executor-mistralai-client` | 图片�?文档�?|
| - | `DashscopeLLMClient` | `prompt-executor-dashscope-client` | 阿里云通义 |

### 2.2.1 Jasmine Provider 实现特点分析

#### OpenAIProvider
- 支持 Chat Completions API �?Responses API（通过 `useResponseApi` 开关）
- **内部结构**：分为两个独立的 API 实现�?
  - `openai/ChatCompletionsAPI.kt` - 标准 Chat Completions API
  - `openai/ResponseAPI.kt` - OpenAI Response API（新版）
- 使用 `KeyRoulette` 实现�?API Key 轮询
- 支持自定�?`chatCompletionsPath`
- 支持余额查询（`getBalance`�?

#### GoogleProvider
- 同时支持 Google AI Studio �?Vertex AI
- Vertex AI 使用 Service Account 认证（`ServiceAccountTokenProvider`，位�?`providers/vertex/`�?
- **Vertex AI 配置字段**：`vertexAI`, `privateKey`, `serviceAccountEmail`, `location`, `projectId`
- 支持内置工具：`google_search`、`url_context`
- 支持 Thinking/Reasoning 模式（Gemini 2.5 Pro�?
- 支持图片生成输出（`responseModalities: ["TEXT", "IMAGE"]`�?

#### ClaudeProvider
- 使用 `anthropic-version: 2023-06-01` �?
- 支持 Thinking 模式（`thinking.type: enabled/disabled`�?
- 支持 `thinking_delta`、`signature_delta` 流式事件
- Tool Result 作为 `user` 角色发�?

### 2.2.2 Koog LLMClient 实现特点

#### OpenAILLMClient
- 支持 Chat Completions �?Responses API
- 内置 `RetryingLLMClient` 装饰器支持重�?
- 支持 `OpenAIChatParams` �?`OpenAIResponsesParams` 参数
- 支持 `reasoningEffort` 参数
- 支持 `webSearchOptions` 内置搜索

#### GoogleLLMClient
- 仅支�?Google AI Studio（不支持 Vertex AI�?
- 支持完整多模态（图片、音频、视频、文档）
- 支持 `thinkingConfig` 参数

#### AnthropicLLMClient
- 支持 PDF 文档处理
- 支持 Thinking 模式

### 2.3 消息模型映射

#### 2.3.1 消息部分类型对比

| Jasmine UIMessagePart | Koog 对应 | 转换说明 |
|----------------------|-----------|---------|
| `Text(text)` | `Message.Content.Text(text)` | 直接映射 |
| `Image(url)` | `Message.Content.Image(path/url)` | 需处理 base64 �?URL |
| `Video(url)` | `Message.Content.Video(path)` | �?Google 支持 |
| `Document(url, fileName, mime)` | `Message.Content.Document(path)` | 需处理文件路径 |
| `Reasoning(reasoning, ...)` | 无直接对�?| 需特殊处理，可能作�?metadata |
| `ToolCall(id, name, args)` | `Message.ToolCall(id, name, args)` | 结构相似 |
| `ToolResult(id, name, content, args)` | `Message.Tool(id, content)` | 需简�?|

#### 2.3.2 关键差异

1. **Reasoning 处理**：Jasmine �?Reasoning 作为独立�?`UIMessagePart`，Koog 没有直接对应，需要：
   - 在适配层保�?Reasoning 信息
   - 或将其作为特殊的 Text 内容处理

2. **ToolResult 结构**�?
   - Jasmine: 包含 `toolCallId`, `toolName`, `content`, `arguments`
   - Koog: 仅包�?`toolCallId`, `content`
   - 需要在适配层保留额外信�?

3. **Metadata 支持**：Jasmine 的每�?`UIMessagePart` 都有 `metadata: JsonObject?`，用于存储额外信息（�?`thoughtSignature`），Koog 没有直接对应

4. **Annotations**：Jasmine 支持 `UIMessageAnnotation.UrlCitation`（用于搜索结果引用），Koog 没有直接对应

#### 2.3.3 流式响应映射

| Jasmine MessageChunk | Koog StreamFrame | 说明 |
|---------------------|------------------|------|
| `choices[0].delta.parts[Text]` | `StreamFrame.Append(text)` | 文本增量 |
| `choices[0].delta.parts[ToolCall]` | `StreamFrame.ToolCall(id, name, args)` | 工具调用 |
| `choices[0].finishReason` | `StreamFrame.End(finishReason, metaInfo)` | 结束信号 |
| `usage: TokenUsage` | `StreamFrame.End.metaInfo.usage` | Token 统计 |

```kotlin
// Jasmine MessageChunk 结构
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>,
    val usage: TokenUsage?
)

data class UIMessageChoice(
    val index: Int,
    val delta: UIMessage?,      // 流式增量
    val message: UIMessage?,    // 完整消息（非流式�?
    val finishReason: String?
)

// Koog StreamFrame 结构
sealed class StreamFrame {
    data class Append(val text: String)
    data class ToolCall(val id: String, val name: String, val args: String)
    data class End(val finishReason: String?, val metaInfo: ResponseMetaInfo?)
}
```


## 3. 迁移策略

### 3.1 迁移方式选择

**推荐方案：渐进式迁移 + 适配�?*

由于 Jasmine �?UI 层和数据层深度依赖现有的 `UIMessage`、`UIMessagePart` 等模型，建议�?

1. **保留现有 UI 消息模型**：`UIMessage`、`UIMessagePart` 等继续使�?
2. **创建适配�?*：在 Koog �?Jasmine 之间建立转换�?
3. **替换底层实现**：用 Koog �?`PromptExecutor` 替换现有 `Provider` 实现
4. **逐步迁移**：先迁移核心聊天功能，再迁移高级特�?

### 3.2 模块依赖变更

```kotlin
// ai/build.gradle.kts 新增依赖
dependencies {
    // Koog 核心
    implementation("ai.koog:koog-agents:0.5.4")
    
    // 或者按需引入特定模块
    implementation("ai.koog:prompt-executor-model:0.5.4")
    implementation("ai.koog:prompt-executor-openai-client:0.5.4")
    implementation("ai.koog:prompt-executor-anthropic-client:0.5.4")
    implementation("ai.koog:prompt-executor-google-client:0.5.4")
    implementation("ai.koog:prompt-executor-ollama-client:0.5.4")
    implementation("ai.koog:prompt-executor-deepseek-client:0.5.4")
    implementation("ai.koog:prompt-executor-openrouter-client:0.5.4")
    
    // MCP 支持（可选）
    implementation("ai.koog:agents-mcp:0.5.4")
    
    // 移除现有 OkHttp SSE 依赖（Koog 使用 Ktor�?
    // api(libs.okhttp.sse) // 可能需要保留用于其他功�?
}
```

### 3.3 版本兼容�?

#### 3.3.1 依赖版本对比

| 依赖 | Jasmine 当前版本 | Koog 要求版本 | 兼容�?|
|-----|-----------------|--------------|--------|
| Kotlin | 2.2.21 | 2.2.10 | �?兼容（Jasmine 更新�?|
| kotlinx-coroutines | 1.10.2 | 1.10.2 | �?完全匹配 |
| kotlinx-serialization | 1.9.0 | 1.8.1 | �?兼容（Jasmine 更新�?|
| kotlinx-datetime | 0.7.1 | 0.6.2 | �?兼容（Jasmine 更新�?|
| Ktor | 3.3.2 | 3.2.2 | �?兼容（Jasmine 更新�?|
| OkHttp | 5.1.0 | - | ⚠️ Koog 使用 Ktor，需评估 |
| MCP SDK | 0.7.7 | 0.7.2 | �?兼容（Jasmine 更新�?|

#### 3.3.2 HTTP 客户端策�?

**当前状�?*�?
- Jasmine 使用 OkHttp 5.1.0 + OkHttp SSE
- Koog 使用 Ktor 3.2.2（支持多种引擎：CIO、OkHttp、Apache5�?

**推荐策略**：混合使�?
```kotlin
// Koog 支持 OkHttp 作为 Ktor 引擎
implementation("io.ktor:ktor-client-okhttp:3.2.2")

// 这样可以�?
// 1. 复用现有 OkHttp 配置（代理、拦截器等）
// 2. 保持与其他模块的兼容性（Retrofit、Coil 等）
```

#### 3.3.3 潜在冲突

1. **Ktor 版本**：Jasmine 已有 Ktor 3.3.2，Koog 要求 3.2.2
   - 解决：统一使用 Jasmine �?3.3.2（向后兼容）

2. **kotlinx-serialization**：Jasmine 1.9.0 vs Koog 1.8.1
   - 解决：使�?Jasmine �?1.9.0（向后兼容）

3. **MCP SDK**：Jasmine 0.7.7 vs Koog 0.7.2
   - 解决：使�?Jasmine �?0.7.7（向后兼容）

**注意**：Jasmine 目前使用 OkHttp，需要评估是否完全切换到 Ktor 或保持混用�?


## 4. 详细迁移步骤（按功能逐个迁移）

> **迁移策略**：采用"逐个 Provider 迁移"的方式，每次只迁移一个 Provider，确保稳定后再迁移下一个。
> 这样可以：1) 降低风险 2) 快速验证 3) 随时回滚 4) 并行开发

### 4.0 迁移顺序规划

| 顺序 | Provider | 优先级 | 原因 |
|-----|----------|--------|------|
| 1 | OpenAI | 高 | 最常用，Koog 支持最完善 |
| 2 | DeepSeek | 高 | 新增 Provider，无需替换旧代码 |
| 3 | Ollama | 中 | 新增 Provider，本地部署场景 |
| 4 | Claude | 中 | 替换现有实现 |
| 5 | Google | 低 | 需要保留 Vertex AI 特殊处理 |
| 6 | OpenRouter | 低 | 新增 Provider，聚合服务 |

---

### 4.1 Phase 0: 基础设施准备

#### 4.1.1 添加 Koog 依赖
```kotlin
// gradle/libs.versions.toml
[versions]
koog = "0.5.4"

[libraries]
koog-agents = { module = "ai.koog:koog-agents", version.ref = "koog" }
koog-prompt-openai = { module = "ai.koog:prompt-executor-openai-client", version.ref = "koog" }
koog-prompt-anthropic = { module = "ai.koog:prompt-executor-anthropic-client", version.ref = "koog" }
koog-prompt-google = { module = "ai.koog:prompt-executor-google-client", version.ref = "koog" }
koog-prompt-ollama = { module = "ai.koog:prompt-executor-ollama-client", version.ref = "koog" }
koog-prompt-deepseek = { module = "ai.koog:prompt-executor-deepseek-client", version.ref = "koog" }
koog-prompt-openrouter = { module = "ai.koog:prompt-executor-openrouter-client", version.ref = "koog" }
```

```kotlin
// ai/build.gradle.kts - 初始只添加核心依赖
dependencies {
    // Koog 核心（先只添加 OpenAI）
    implementation(libs.koog.prompt.openai)
}
```

#### 4.1.2 创建适配层基础结构
```
ai/src/main/java/com/lhzkmlai/    # 注意包名
├── koog/                    # 新增：Koog 适配�?
�?  ├── adapter/             # 消息转换适配�?
�?  �?  └── MessageAdapter.kt
�?  ├── provider/            # Koog Provider 实现
�?  �?  └── KoogOpenAIProvider.kt  # 第一个迁�?
�?  └── util/
�?      └── KoogModelMapper.kt
├── provider/                # 保留：原有实现继续工�?
└── ...
```

#### 4.1.3 创建通用 MessageAdapter
```kotlin
// ai/src/main/java/com/lhzkmlai/koog/adapter/MessageAdapter.kt
object MessageAdapter {
    // UIMessage -> Koog Prompt
    fun toKoogPrompt(messages: List<UIMessage>): Prompt {
        return prompt {
            messages.forEach { msg ->
                when (msg.role) {
                    MessageRole.SYSTEM -> system(msg.toText())
                    MessageRole.USER -> user { 
                        msg.parts.forEach { part ->
                            when (part) {
                                is UIMessagePart.Text -> text(part.text)
                                is UIMessagePart.Image -> image(part.url)
                                else -> {} // 其他类型按需处理
                            }
                        }
                    }
                    MessageRole.ASSISTANT -> assistant(msg.toText())
                    MessageRole.TOOL -> {
                        val result = msg.getToolResults().firstOrNull()
                        if (result != null) {
                            tool(result.toolCallId, result.content.toString())
                        }
                    }
                }
            }
        }
    }
    
    // Koog StreamFrame -> MessageChunk
    fun toMessageChunk(frame: StreamFrame, model: String): MessageChunk {
        return when (frame) {
            is StreamFrame.Append -> MessageChunk(
                id = "",
                model = model,
                choices = listOf(UIMessageChoice(
                    index = 0,
                    delta = UIMessage.assistant(listOf(UIMessagePart.Text(frame.text))),
                    message = null,
                    finishReason = null
                )),
                usage = null
            )
            is StreamFrame.ToolCall -> MessageChunk(
                id = "",
                model = model,
                choices = listOf(UIMessageChoice(
                    index = 0,
                    delta = UIMessage.assistant(listOf(
                        UIMessagePart.ToolCall(frame.id, frame.name, frame.args)
                    )),
                    message = null,
                    finishReason = null
                )),
                usage = null
            )
            is StreamFrame.End -> MessageChunk(
                id = "",
                model = model,
                choices = listOf(UIMessageChoice(
                    index = 0,
                    delta = null,
                    message = null,
                    finishReason = frame.finishReason
                )),
                usage = frame.metaInfo?.usage?.let { 
                    TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
                }
            )
        }
    }
}
```

---

### 4.2 Phase 1: 迁移 OpenAI Provider

#### 4.2.1 创建 KoogOpenAIProvider
```kotlin
// ai/src/main/java/com/lhzkmlai/koog/provider/KoogOpenAIProvider.kt
class KoogOpenAIProvider : Provider<ProviderSetting.OpenAI> {
    
    private fun createClient(setting: ProviderSetting.OpenAI): OpenAILLMClient {
        // 支持 Key 轮询
        val apiKey = KeyRoulette.default().next(setting.apiKey)
        
        return OpenAILLMClient.builder()
            .apiKey(apiKey)
            .baseUrl(setting.baseUrl.ifEmpty { "https://api.openai.com/v1" })
            .build()
    }
    
    override suspend fun listModels(providerSetting: ProviderSetting.OpenAI): List<Model> {
        // 保留原实现或使用 Koog
        return emptyList() // TODO: Koog 暂不支持 listModels
    }
    
    override suspend fun getBalance(providerSetting: ProviderSetting.OpenAI): String {
        // 保留原实现，Koog 不支�?
        return OpenAIProvider(OkHttpClient()).getBalance(providerSetting)
    }
    
    override suspend fun generateText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk {
        val client = createClient(providerSetting)
        val prompt = MessageAdapter.toKoogPrompt(messages)
        val model = LLModel(params.model.modelId)
        
        val response = client.execute(prompt, model)
        return MessageChunk(
            id = "",
            model = params.model.modelId,
            choices = listOf(UIMessageChoice(
                index = 0,
                delta = null,
                message = UIMessage.assistant(listOf(UIMessagePart.Text(response))),
                finishReason = "stop"
            )),
            usage = null
        )
    }
    
    override suspend fun streamText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> {
        val client = createClient(providerSetting)
        val prompt = MessageAdapter.toKoogPrompt(messages)
        val model = LLModel(params.model.modelId)
        
        return client.executeStreaming(prompt, model)
            .map { frame -> MessageAdapter.toMessageChunk(frame, params.model.modelId) }
    }
}
```

#### 4.2.2 添加特性开�?
```kotlin
// ai/src/main/java/com/lhzkml/ai/provider/ProviderManager.kt
class ProviderManager(client: OkHttpClient) {
    companion object {
        // 特性开关：控制是否使用 Koog 实现
        var useKoogOpenAI = false  // 默认关闭，测试通过后开�?
    }
    
    init {
        // OpenAI: 根据开关选择实现
        if (useKoogOpenAI) {
            registerProvider("openai", KoogOpenAIProvider())
        } else {
            registerProvider("openai", OpenAIProvider(client))
        }
        
        // 其他 Provider 保持原实�?
        registerProvider("google", GoogleProvider(client))
        registerProvider("claude", ClaudeProvider(client))
    }
}
```

#### 4.2.3 测试验证
- [ ] 基本文本生成
- [ ] 流式输出
- [ ] 多轮对话
- [ ] 工具调用
- [ ] 多模态（图片输入�?
- [ ] Response API 模式
- [ ] Key 轮询

---

### 4.3 Phase 2: 新增 DeepSeek Provider

> DeepSeek 是新增的 Provider，不需要替换旧代码，风险最�?

#### 4.3.1 添加依赖
```kotlin
// ai/build.gradle.kts
dependencies {
    implementation(libs.koog.prompt.deepseek)
}
```

#### 4.3.2 添加 ProviderSetting
```kotlin
// ai/src/main/java/com/lhzkmlai/provider/ProviderSetting.kt
@Serializable @SerialName("deepseek")
data class DeepSeek(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "DeepSeek",
    override var models: List<Model> = listOf(
        Model(modelId = "deepseek-chat", displayName = "DeepSeek Chat"),
        Model(modelId = "deepseek-coder", displayName = "DeepSeek Coder"),
        Model(modelId = "deepseek-reasoner", displayName = "DeepSeek Reasoner", abilities = listOf(ModelAbility.REASONING)),
    ),
    override var proxy: ProviderProxy = ProviderProxy.None,
    override val balanceOption: BalanceOption = BalanceOption(),
    // UI 相关字段（@Transient�?
    @kotlinx.serialization.Transient override val builtIn: Boolean = false,
    @kotlinx.serialization.Transient override val description: @Composable (() -> Unit) = {},
    @kotlinx.serialization.Transient override val shortDescription: @Composable (() -> Unit) = {},
    // DeepSeek 特有字段
    var apiKey: String = "",
    var baseUrl: String = "https://api.deepseek.com",
) : ProviderSetting() {
    // 必须实现的抽象方�?
    override fun addModel(model: Model) = copy(models = models + model)
    override fun editModel(model: Model) = copy(models = models.map { if (it.id == model.id) model else it })
    override fun delModel(model: Model) = copy(models = models.filter { it.id != model.id })
    override fun moveMove(from: Int, to: Int) = copy(models = models.toMutableList().apply {
        val m = removeAt(from); add(to, m)
    })
    override fun copyProvider(id: Uuid, enabled: Boolean, name: String, models: List<Model>,
        proxy: ProviderProxy, balanceOption: BalanceOption, builtIn: Boolean,
        description: @Composable (() -> Unit), shortDescription: @Composable (() -> Unit)
    ) = copy(id = id, enabled = enabled, name = name, models = models, proxy = proxy,
        balanceOption = balanceOption, builtIn = builtIn, description = description, shortDescription = shortDescription)
}
```

#### 4.3.3 创建 KoogDeepSeekProvider
```kotlin
// ai/src/main/java/com/lhzkmlai/koog/provider/KoogDeepSeekProvider.kt
class KoogDeepSeekProvider : Provider<ProviderSetting.DeepSeek> {
    
    private fun createClient(setting: ProviderSetting.DeepSeek): DeepSeekLLMClient {
        return DeepSeekLLMClient.builder()
            .apiKey(setting.apiKey)
            .baseUrl(setting.baseUrl)
            .build()
    }
    
    override suspend fun streamText(
        providerSetting: ProviderSetting.DeepSeek,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> {
        val client = createClient(providerSetting)
        val prompt = MessageAdapter.toKoogPrompt(messages)
        val model = LLModel(params.model.modelId)
        
        return client.executeStreaming(prompt, model)
            .map { frame -> MessageAdapter.toMessageChunk(frame, params.model.modelId) }
    }
    
    // ... 其他方法类似
}
```

#### 4.3.4 注册 Provider
```kotlin
// ProviderManager.kt
init {
    // ... 现有 Provider
    registerProvider("deepseek", KoogDeepSeekProvider())
}

// ProviderSetting.kt companion object
val Types by lazy {
    listOf(
        OpenAI::class,
        Google::class,
        Claude::class,
        DeepSeek::class,  // 新增
    )
}
```

---

### 4.4 Phase 3: 新增 Ollama Provider

> Ollama 用于本地部署，也是新�?Provider

#### 4.4.1 添加依赖和配�?
```kotlin
// ai/build.gradle.kts
dependencies {
    implementation(libs.koog.prompt.ollama)
}
```

#### 4.4.2 添加 ProviderSetting
```kotlin
@Serializable @SerialName("ollama")
data class Ollama(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "Ollama",
    override var models: List<Model> = emptyList(),  // 动态获�?
    override var proxy: ProviderProxy = ProviderProxy.None,  // Ollama 通常不需要代�?
    override val balanceOption: BalanceOption = BalanceOption(enabled = false),  // 本地部署无余�?
    @kotlinx.serialization.Transient override val builtIn: Boolean = false,
    @kotlinx.serialization.Transient override val description: @Composable (() -> Unit) = {},
    @kotlinx.serialization.Transient override val shortDescription: @Composable (() -> Unit) = {},
    var baseUrl: String = "http://localhost:11434",
) : ProviderSetting() {
    // 必须实现的抽象方法（�?DeepSeek�?
    override fun addModel(model: Model) = copy(models = models + model)
    override fun editModel(model: Model) = copy(models = models.map { if (it.id == model.id) model else it })
    override fun delModel(model: Model) = copy(models = models.filter { it.id != model.id })
    override fun moveMove(from: Int, to: Int) = copy(models = models.toMutableList().apply {
        val m = removeAt(from); add(to, m)
    })
    override fun copyProvider(id: Uuid, enabled: Boolean, name: String, models: List<Model>,
        proxy: ProviderProxy, balanceOption: BalanceOption, builtIn: Boolean,
        description: @Composable (() -> Unit), shortDescription: @Composable (() -> Unit)
    ) = copy(id = id, enabled = enabled, name = name, models = models, proxy = proxy,
        balanceOption = balanceOption, builtIn = builtIn, description = description, shortDescription = shortDescription)
}
```

#### 4.4.3 创建 KoogOllamaProvider
```kotlin
class KoogOllamaProvider : Provider<ProviderSetting.Ollama> {
    
    override suspend fun listModels(providerSetting: ProviderSetting.Ollama): List<Model> {
        // Ollama 支持动态获取模型列�?
        val client = OllamaLLMClient.builder()
            .baseUrl(providerSetting.baseUrl)
            .build()
        // TODO: 调用 Ollama API 获取模型列表
        return emptyList()
    }
    
    override suspend fun streamText(...): Flow<MessageChunk> {
        val client = OllamaLLMClient.builder()
            .baseUrl(providerSetting.baseUrl)
            .build()
        // ... 实现
    }
}
```

---

### 4.5 Phase 4: 迁移 Claude Provider

#### 4.5.1 添加依赖
```kotlin
dependencies {
    implementation(libs.koog.prompt.anthropic)
}
```

#### 4.5.2 创建 KoogClaudeProvider
```kotlin
class KoogClaudeProvider : Provider<ProviderSetting.Claude> {
    
    private fun createClient(setting: ProviderSetting.Claude): AnthropicLLMClient {
        return AnthropicLLMClient.builder()
            .apiKey(setting.apiKey)
            .baseUrl(setting.baseUrl.ifEmpty { "https://api.anthropic.com" })
            .build()
    }
    
    override suspend fun streamText(
        providerSetting: ProviderSetting.Claude,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> {
        val client = createClient(providerSetting)
        val prompt = MessageAdapter.toKoogPrompt(messages)
        
        // 处理 Thinking 模式
        val koogParams = if (params.thinking) {
            AnthropicParams(thinkingEnabled = true)
        } else null
        
        return client.executeStreaming(prompt, LLModel(params.model.modelId), koogParams)
            .map { frame -> 
                // 特殊处理 Reasoning
                MessageAdapter.toMessageChunkWithReasoning(frame, params.model.modelId)
            }
    }
}
```

#### 4.5.3 特殊处理：Reasoning/Thinking
```kotlin
// MessageAdapter.kt 扩展
fun toMessageChunkWithReasoning(frame: StreamFrame, model: String): MessageChunk {
    // Claude �?thinking 需要特殊处�?
    // �?thinking 内容转换�?UIMessagePart.Reasoning
    // ...
}
```

#### 4.5.4 添加特性开关并测试
```kotlin
companion object {
    var useKoogOpenAI = true   // 已验�?
    var useKoogClaude = false  // 新增，默认关�?
}
```

---

### 4.6 Phase 5: 迁移 Google Provider

> Google Provider 最复杂，需要保�?Vertex AI 支持

#### 4.6.1 策略：分�?AI Studio �?Vertex AI
```kotlin
// 方案：Koog 处理 AI Studio，保留原实现处理 Vertex AI
class KoogGoogleProvider : Provider<ProviderSetting.Google> {
    private val legacyProvider = GoogleProvider(OkHttpClient())  // 保留原实�?
    
    override suspend fun streamText(
        providerSetting: ProviderSetting.Google,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> {
        // 判断是否使用 Vertex AI
        if (providerSetting.vertexAI && providerSetting.privateKey.isNotEmpty()) {
            // 使用原实现处�?Vertex AI
            return legacyProvider.streamText(providerSetting, messages, params)
        }
        
        // 使用 Koog 处理 AI Studio
        val client = GoogleLLMClient.builder()
            .apiKey(providerSetting.apiKey)
            .build()
        
        return client.executeStreaming(prompt, model)
            .map { frame -> MessageAdapter.toMessageChunk(frame, params.model.modelId) }
    }
}
```

#### 4.6.2 保留的特殊功�?
- Vertex AI Service Account 认证
- 内置工具（google_search, url_context�?
- 图片生成输出

---

### 4.7 Phase 6: 新增 OpenRouter Provider

```kotlin
@Serializable @SerialName("openrouter")
data class OpenRouter(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "OpenRouter",
    override var models: List<Model> = emptyList(),
    var apiKey: String = "",
) : ProviderSetting()

class KoogOpenRouterProvider : Provider<ProviderSetting.OpenRouter> {
    // 实现类似 OpenAI
}
```

---

### 4.8 Phase 7: 清理与优化

> **重要**：迁移完成后必须删除旧代码，只保留 Koog 不支持的功能

#### 4.8.1 AI 模块 - 必须删除的文件

| 文件/目录 | 删除原因 |
|----------|---------|
| `ai/.../provider/providers/OpenAIProvider.kt` | 已被 KoogOpenAIProvider 替代 |
| `ai/.../provider/providers/openai/` | OpenAI 子模块，随 OpenAIProvider 一起删除 |
| `ai/.../provider/providers/ClaudeProvider.kt` | 已被 KoogClaudeProvider 替代 |
| `ai/.../provider/providers/GoogleProvider.kt` | 已被 KoogGoogleProvider 替代（Vertex AI 逻辑移入 Koog 适配层） |
| `ai/.../provider/providers/vertex/` | Vertex AI 认证逻辑移入 KoogGoogleProvider |
| `ai/.../util/KeyRoulette.kt` | 如果 Koog 支持多 Key 轮询则删除，否则保留 |

#### 4.8.2 AI 模块 - 保留的文件（Koog 不支持）

| 文件 | 保留原因 |
|-----|---------|
| `ai/.../core/Tool.kt` | Jasmine 的 Tool 定义，用于适配层转换 |
| `ai/.../core/MessageRole.kt` | 消息角色枚举，UI 层依赖 |
| `ai/.../core/Usage.kt` | Token 统计，UI 层依赖 |
| `ai/.../ui/Message.kt` | UIMessage 模型，UI 层深度依赖 |
| `ai/.../provider/Model.kt` | Model 定义，UI 层依赖 |
| `ai/.../provider/ProviderSetting.kt` | Provider 配置，UI 层依赖 |
| `ai/.../provider/ProviderManager.kt` | 保留但重构为使用 Koog Provider |
| `ai/.../registry/` | 模型匹配器，保留 |
| `ai/.../mnn/` | 本地模型支持，与 Koog 无关 |
| `ai/.../util/ProxyUtils.kt` | 代理配置，Koog 可能需要 |
| `ai/.../util/FileEncoder.kt` | 文件编码，与 Koog 无关 |

#### 4.8.3 App 模块 - 工具迁移

现有工具需要从 Jasmine Tool 格式转换为 Koog Tool 格式：

| 文件 | 处理方式 |
|-----|---------|
| `app/.../data/ai/tools/LocalTools.kt` | 重写为 Koog Tool 格式 |
| `app/.../data/ai/tools/FileSystemTools.kt` | 重写为 Koog Tool 格式 |
| `app/.../data/ai/tools/MarkdownFileTool.kt` | 重写为 Koog Tool 格式 |

**工具迁移示例**：
```kotlin
// 旧的 Jasmine Tool 格式
val javascriptTool = Tool(
    name = "eval_javascript",
    description = "Execute JavaScript code",
    parameters = { InputSchema.Obj(...) },
    execute = { args -> ... }
)

// 新的 Koog Tool 格式
val javascriptTool = simpleTool(
    name = "eval_javascript",
    description = "Execute JavaScript code"
) { args: EvalJsArgs ->
    // 执行逻辑
}

// 或者使用注解式
class LocalToolSet : ToolSet {
    @Tool
    @LLMDescription("Execute JavaScript code")
    fun evalJavascript(@LLMDescription("The code to execute") code: String): String {
        // 执行逻辑
    }
}
```

#### 4.8.4 App 模块 - 保留的文件

| 文件/目录 | 保留原因 |
|----------|---------|
| `app/.../data/ai/GenerationHandler.kt` | 核心生成逻辑，需要适配 Koog 但保留 |
| `app/.../data/ai/transformers/` | 消息转换器，与 Koog 无关 |
| `app/.../data/ai/prompts/` | 提示词模板，与 Koog 无关 |
| `app/.../data/ai/mcp/` | MCP 集成，可能需要适配 Koog MCP |
| `app/.../data/ai/AILogging.kt` | 日志记录，保留 |

#### 4.8.5 移除特性开关

```kotlin
// 所有 Provider 验证通过后，移除特性开关
class ProviderManager(client: OkHttpClient) {
    init {
        // 全部使用 Koog 实现
        registerProvider("openai", KoogOpenAIProvider())
        registerProvider("google", KoogGoogleProvider())
        registerProvider("claude", KoogClaudeProvider())
        registerProvider("deepseek", KoogDeepSeekProvider())
        registerProvider("ollama", KoogOllamaProvider())
        registerProvider("openrouter", KoogOpenRouterProvider())
    }
}
```

#### 4.8.6 依赖清理

```kotlin
// ai/build.gradle.kts
dependencies {
    // 删除 OkHttp SSE（Koog 使用 Ktor）
    // api(libs.okhttp.sse)  // 删除
    
    // 保留 OkHttp（其他模块可能需要）
    api(libs.okhttp)
    
    // Koog 依赖
    implementation(libs.koog.prompt.openai)
    implementation(libs.koog.prompt.anthropic)
    implementation(libs.koog.prompt.google)
    implementation(libs.koog.prompt.deepseek)
    implementation(libs.koog.prompt.ollama)
    implementation(libs.koog.prompt.openrouter)
}
```




## 5. 需要修改的文件清单

### 5.1 构建配置
| 文件 | 修改内容 |
|-----|---------|
| `gradle/libs.versions.toml` | 添加 Koog 版本和依赖 |
| `ai/build.gradle.kts` | 添加 Koog 依赖，移除 okhttp-sse |
| `app/build.gradle.kts` | 可能需要调整依赖传递 |

### 5.2 AI 模块（新增）
| 文件 | 说明 |
|-----|------|
| `ai/.../koog/adapter/MessageAdapter.kt` | UIMessage <-> Koog Prompt 转换 |
| `ai/.../koog/adapter/ToolAdapter.kt` | Jasmine Tool -> Koog Tool 转换 |
| `ai/.../koog/provider/KoogOpenAIProvider.kt` | OpenAI Koog 实现 |
| `ai/.../koog/provider/KoogClaudeProvider.kt` | Claude Koog 实现 |
| `ai/.../koog/provider/KoogGoogleProvider.kt` | Google Koog 实现（含 Vertex AI） |
| `ai/.../koog/provider/KoogDeepSeekProvider.kt` | DeepSeek Koog 实现 |
| `ai/.../koog/provider/KoogOllamaProvider.kt` | Ollama Koog 实现 |
| `ai/.../koog/provider/KoogOpenRouterProvider.kt` | OpenRouter Koog 实现 |

### 5.3 AI 模块（修改）
| 文件 | 修改内容 |
|-----|---------|
| `ai/.../provider/ProviderSetting.kt` | 新增 DeepSeek/Ollama/OpenRouter 类型 |
| `ai/.../provider/ProviderManager.kt` | 使用 Koog Provider 替换旧实现 |

### 5.4 AI 模块（删除）
| 文件 | 删除原因 |
|-----|---------|
| `ai/.../provider/providers/OpenAIProvider.kt` | 被 KoogOpenAIProvider 替代 |
| `ai/.../provider/providers/openai/` | 整个目录删除 |
| `ai/.../provider/providers/ClaudeProvider.kt` | 被 KoogClaudeProvider 替代 |
| `ai/.../provider/providers/GoogleProvider.kt` | 被 KoogGoogleProvider 替代 |
| `ai/.../provider/providers/vertex/` | 逻辑移入 KoogGoogleProvider |

### 5.5 App 模块（修改）
| 文件 | 修改内容 |
|-----|---------|
| `app/.../data/ai/GenerationHandler.kt` | 适配 Koog（保留核心逻辑） |
| `app/.../data/ai/tools/LocalTools.kt` | 重写为 Koog Tool 格式 |
| `app/.../data/ai/tools/FileSystemTools.kt` | 重写为 Koog Tool 格式 |
| `app/.../data/ai/tools/MarkdownFileTool.kt` | 重写为 Koog Tool 格式 |
| `app/.../ui/pages/setting/SettingProviderPage.kt` | 新增 Provider 类型 UI |
| `app/.../ui/pages/setting/SettingProviderDetailPage.kt` | 新增 Provider 配置 UI |
| `app/.../data/datastore/DefaultProviders.kt` | 新增默认 Provider 配置 |

### 5.5 AI 模块（可删除�?
| 文件 | 说明 |
|-----|------|
| `ai/.../provider/providers/OpenAIProvider.kt` | �?Koog 替代 |
| `ai/.../provider/providers/openai/` | OpenAI 子模块，�?OpenAIProvider 一起删�?|
| `ai/.../provider/providers/ClaudeProvider.kt` | �?Koog 替代 |
| `ai/.../provider/providers/GoogleProvider.kt` | **保留**（Vertex AI 支持�?|


## 6. 功能差异与特殊处�?

### 6.1 Jasmine 独有功能（需特殊处理�?

| 功能 | Jasmine 实现 | Koog 支持 | 处理方案 |
|-----|-------------|----------|---------|
| **Vertex AI** | `GoogleProvider` 支持 Service Account 认证 | �?不支�?| 保留�?GoogleProvider 或扩�?Koog |
| **余额查询** | `Provider.getBalance()` | �?不支�?| 保留原实现，独立�?Koog |
| **自定�?Headers** | `TextGenerationParams.customHeaders` | ⚠️ 部分支持 | 需要扩�?Koog 客户端配�?|
| **自定�?Body** | `TextGenerationParams.customBody` | ⚠️ 部分支持 | 需要扩�?Koog 客户端配�?|
| **Key 轮询** | `KeyRoulette` �?API Key 轮询 | �?不支�?| 保留原实现，在适配层处�?|
| **Reasoning Metadata** | `UIMessagePart.Reasoning.metadata` 存储 `signature` | �?不支�?| 在适配层保�?|
| **Response API** | OpenAI `useResponseApi` 开�?| �?支持 | 使用 `OpenAIResponsesParams` |
| **内置工具** | Google `google_search`, `url_context` | ⚠️ 部分支持 | 需要验�?Koog 支持情况 |

### 6.2 Koog 新增功能（可利用�?

| 功能 | 说明 | 价�?|
|-----|------|------|
| **RetryingLLMClient** | 内置重试逻辑，支持多种策�?| 提高稳定�?|
| **DefaultMultiLLMPromptExecutor** | �?LLM 编排，自动切�?| 提高可用�?|
| **CachedPromptExecutor** | 响应缓存 | 降低成本 |
| **结构化输�?* | `executeWithStructuredOutput<T>()` | 简化解�?|
| **Agent 系统** | 图策略、状态管�?| 复杂任务支持 |
| **OpenTelemetry** | 可观测性集�?| 监控调试 |
| **Memory 特�?* | 跨会话记�?| 增强上下�?|
| **更多 Provider** | DeepSeek、Ollama、OpenRouter、Bedrock、Mistral | 更多选择 |

### 6.3 需要保留的原实�?

基于功能差异分析，以下功能建议保留原实现�?

```kotlin
// 1. Vertex AI 支持 - 保留 GoogleProvider 中的 Vertex AI 逻辑
class VertexAIProvider(private val client: OkHttpClient) : Provider<ProviderSetting.Google> {
    // 保留 ServiceAccountTokenProvider
    // 保留 Vertex AI URL 构建逻辑
}

// 2. 余额查询 - 独立实现
interface BalanceQueryService {
    suspend fun getBalance(providerSetting: ProviderSetting): String
}

// 3. Key 轮询 - 在适配层使�?
class KoogClientFactory {
    private val keyRoulette = KeyRoulette.default()
    
    fun createClient(setting: ProviderSetting.OpenAI): OpenAILLMClient {
        val apiKey = keyRoulette.next(setting.apiKey)  // 轮询选择 key
        return OpenAILLMClient(apiKey = apiKey, ...)
    }
}
```

## 7. 风险与挑�?

### 7.1 技术风�?

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| HTTP 客户端差�?| Koog 使用 Ktor，Jasmine 使用 OkHttp | 使用 `ktor-client-okhttp` 引擎 |
| 消息模型差异 | 转换可能丢失信息 | 仔细设计适配层，保留所有必要字�?|
| 流式处理差异 | Koog StreamFrame �?MessageChunk 结构不同 | 创建完整的转换逻辑 |
| 版本冲突 | kotlinx 库版本可能冲�?| 统一使用较新版本（向后兼容） |
| Android 兼容�?| Koog 主要面向 JVM | 验证 Android 目标的兼容�?|

### 7.2 功能风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 自定�?Header/Body | Jasmine 支持自定义请求头和请求体 | 扩展 Koog 客户端或保留部分原实�?|
| Proxy 支持 | Jasmine 支持 HTTP 代理 | 使用 Ktor OkHttp 引擎配置代理 |
| 余额查询 | Jasmine 有余额查询功�?| 保留原实现，独立�?Koog |
| Vertex AI | Jasmine 支持 Google Vertex AI | 保留�?GoogleProvider �?Vertex AI 部分 |
| Reasoning Metadata | Jasmine 存储 thinking signature | 在适配层保留，不传递给 Koog |

### 7.3 迁移风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 回归问题 | 迁移可能引入 bug | 充分测试，保留回滚能�?|
| 性能变化 | Ktor vs OkHttp 性能差异 | 使用 OkHttp 引擎，性能一�?|
| 用户数据兼容 | Provider 配置序列化格式变�?| 新增类型使用�?SerialName，不影响现有 |


## 7. 迁移收益

### 7.1 新增功能
- **更多 LLM 提供�?*：DeepSeek、Ollama、OpenRouter、Bedrock、Mistral、Dashscope
- **Agent 能力**：图工作流、状态管理、复杂任务编�?
- **结构化输�?*：原生支�?JSON Schema 输出
- **历史压缩**：自动优化长对话 Token 使用
- **�?LLM 编排**：支持在多个提供商间切换和负载均�?
- **可观测�?*：OpenTelemetry 集成，支�?Langfuse、W&B Weave

### 7.2 代码质量提升
- **减少维护负担**：不再需要维护多�?Provider 实现
- **统一抽象**：使�?Koog 的统一 API
- **更好的测�?*：Koog 提供 Mock 工具
- **类型安全**：Koog 的工具系统提供更好的类型安全

### 7.3 未来扩展
- **A2A 协议**：Agent 间通信能力
- **RAG 支持**：向量存储和检索增强生�?
- **持久�?*：Agent 状态持久化和恢�?
- **记忆系统**：跨会话记忆能力

## 8. 测试策略

### 8.1 单元测试
- 测试 MessageAdapter 的双向转�?
- 测试 ToolAdapter 的工具转�?
- 测试 KoogExecutorFactory 的客户端创建

### 8.2 集成测试
- 测试�?Provider 的基本聊天功�?
- 测试流式输出
- 测试工具调用
- 测试 MCP 工具集成

### 8.3 回归测试
- 确保现有功能不受影响
- 对比迁移前后的响应一致�?
- 性能对比测试

## 9. 回滚方案

如果迁移过程中遇到严重问题，可以�?

1. **保留原实�?*：在迁移期间保留原有 Provider 实现
2. **特性开�?*：通过配置切换使用 Koog 或原实现
3. **版本控制**：使�?Git 分支管理，随时可回滚

```kotlin
// 示例：特性开�?
class ProviderManager(client: OkHttpClient) {
    private val useKoog = BuildConfig.USE_KOOG_FRAMEWORK
    
    init {
        if (useKoog) {
            // 使用 Koog 实现
            registerProvider("openai", KoogProvider(...))
        } else {
            // 使用原实现
            registerProvider("openai", OpenAIProvider(client))
        }
    }
}
```

## 10. 后续优化方向

迁移完成后，可以进一步利用 Koog 的能力：

1. **Agent 工作流**：使用 Koog 的图策略实现复杂对话流程
2. **记忆系统**：利用 Koog 的 Memory 特性增强记忆功能
3. **可观测性**：集成 OpenTelemetry 进行监控
4. **RAG 集成**：利用 Koog 的向量存储实现知识检索
5. **A2A 协议**：实现多 Agent 协作

## 11. 参考资源

- Koog 官方文档：https://docs.koog.ai
- Koog GitHub：https://github.com/JetBrains/koog
- Koog API 文档：https://api.koog.ai
- 示例代码：`koog-0.5.4/examples/`
- DeepWiki 文档：https://deepwiki.com/JetBrains/koog

## 12. 总结与建议

### 12.1 推荐迁移顺序

1. **Phase 1**：添�?Koog 依赖，验证编译通过
2. **Phase 2**：实�?MessageAdapter，验证消息转�?
3. **Phase 3**：实�?KoogProvider for OpenAI，验证基本聊�?
4. **Phase 4**：扩展到 Google、Claude
5. **Phase 5**：添加新 Provider（DeepSeek、Ollama、OpenRouter�?
6. **Phase 6**：迁移工具系�?
7. **Phase 7**：清理旧代码

### 12.2 关键决策点

1. **Vertex AI 支持**：是否保留原 GoogleProvider 的 Vertex AI 实现？
   - 建议：保留，Koog 不支持 Vertex AI

2. **余额查询**：是否保留原实现？
   - 建议：保留，作为独立服务

3. **Key 轮询**：是否在适配层实现？
   - 建议：是，在 KoogClientFactory 中实现

4. **HTTP 客户端**：使用 Ktor 还是 OkHttp？
   - 建议：使用 `ktor-client-okhttp` 引擎，复用现有配置

### 12.3 下一步行动

1. 创建 `ai/src/main/java/com/lhzkmlai/koog/` 目录结构
2. 更新 `gradle/libs.versions.toml` 添加 Koog 依赖
3. 实现 `MessageAdapter` 并编写单元测试
4. 实现 `KoogProvider` 并进行集成测试

---

**文档版本**：3.0  
**创建日期**：2024-12-05  
**更新日期**：2024-12-05  
**作者**：Kiro AI Assistant

### 变更记录

| 版本 | 日期 | 变更内容 |
|-----|------|---------|
| 1.0 | 2024-12-05 | 初始版本 |
| 2.0 | 2024-12-05 | 添加详细源码分析、依赖兼容性分析、功能差异分析 |
| 3.0 | 2024-12-05 | 核对本地代码后修正：包名、ProviderSetting 完整接口、Model 字段、OpenAI 子模块结构 |
| 4.0 | 2024-12-05 | 完善清理方案：明确删除旧代码清单、工具迁移方案、依赖清理 |
